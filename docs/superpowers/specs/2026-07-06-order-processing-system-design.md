# E-commerce Order Processing System — Design

- **Date:** 2026-07-06
- **Status:** Approved
- **Stack:** Java 17, Spring Boot 3.3.x, PostgreSQL, Gradle (Kotlin DSL)
- **Scope:** Single-instance backend for order processing with inventory reservation. No auth, no payment, no messaging.

## 1. Objective

Build a backend for an E-commerce Order Processing System. Customers can place orders with multiple items, retrieve order details, list orders (optionally filtered by status), update order status, and cancel orders. A background scheduler advances `PENDING` orders to `PROCESSING` every 5 minutes. An inventory subsystem reserves stock on order creation, releases it on cancellation, and finalizes it when an order is marked `SHIPPED`.

## 2. Non-Goals (YAGNI)

- Authentication / authorization / roles.
- Payment processing or `PENDING_PAYMENT` state.
- Pagination on `GET /orders` (full list returned).
- Distributed scheduler locking / horizontal scaling.
- Message queue, job store, or retry framework.
- Custom metrics beyond Spring Boot Actuator defaults.
- Lombok (transparent code for the reviewer).
- CI pipeline configuration.

## 3. Architecture

Single Spring Boot module, classic layered monolith. Packages per subsystem; services depend on each other only via interfaces (never by reaching into another package's repository). Controllers never call repositories directly.

```
com.peerislands.orders
├── common/        exception classes, GlobalExceptionHandler, ApiError DTO
├── inventory/     Product entity, repository, service (+interface), controller, DTOs
├── orders/        Order + OrderItem entities, repository, service (+interface), controller, DTOs, OrderStatus enum
│   └── scheduler/ PendingOrderScheduler (@Scheduled)
└── OrdersApplication.java   (@EnableScheduling)
```

### 3.1 Entities

**Product**
```
id            UUID (PK)
name          VARCHAR NOT NULL
sku           VARCHAR UNIQUE NOT NULL
unitPrice     NUMERIC(19,4) NOT NULL
availableStock INT NOT NULL      -- units sellable right now
reservedStock INT NOT NULL DEFAULT 0  -- units held by pending/processing orders
version       BIGINT NOT NULL    -- @Version for optimistic locking
```
Invariant: `availableStock >= 0`, `reservedStock >= 0`.

**Order**
```
id            UUID (PK)
customerName  VARCHAR NOT NULL
status        VARCHAR NOT NULL  -- OrderStatus enum, mapped via @Enumerated(EnumType.STRING)
createdAt     TIMESTAMP NOT NULL
updatedAt     TIMESTAMP NOT NULL
version       BIGINT NOT NULL   -- @Version for optimistic locking
items         List<OrderItem> (OneToMany, cascade ALL, orphanRemoval)
```

**OrderItem**
```
id            UUID (PK)
order         ManyToOne(Order) FK
product       ManyToOne(Product) FK, LAZY
productName   VARCHAR NOT NULL   -- snapshot at create time
unitPrice     NUMERIC(19,4) NOT NULL  -- snapshot at create time
quantity      INT NOT NULL
```
- `productName` and `unitPrice` are **snapshotted** from `Product` at order creation. Later product edits do not alter historical orders.
- `OrderItem` itself is the reservation record; we do not maintain a separate per-line reservation table.

**OrderStatus enum** with a `canTransitionTo(OrderStatus)` method:
```
PENDING     → PROCESSING   (scheduler OR manual PUT)
PROCESSING  → SHIPPED      (manual PUT only; finalizes stock)
SHIPPED     → DELIVERED    (manual PUT only)
ANY         → CANCELLED    (POST /cancel; valid only from PENDING — enforced in service)
```
All other transitions are rejected with `IllegalOrderTransitionException` (→ 400).

### 3.2 Beans & Infrastructure

- `@SpringBootApplication` + `@EnableScheduling`.
- Spring Data JPA; Flyway under `src/main/resources/db/migration`.
- `@Version` (long) on `Order` and `Product`.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) maps:
  - `ResourceNotFoundException` → 404
  - `IllegalOrderTransitionException` → 400
  - `InsufficientStockException` → 409
  - `DuplicateSkuException` → 409
  - `ActiveReservationException` (delete blocked) → 409
  - `OptimisticLockingFailureException` → 409
  - `MethodArgumentNotValidException` → 400 (field errors in message)
  - fallback `Exception` → 500
- All errors share one shape:
  ```
  { "timestamp": ISO-8601, "status": int, "error": "string", "message": "string", "path": "string" }
  ```

### 3.3 Profiles

- Default (`application.yml`): Postgres at `jdbc:postgresql://localhost:5432/orders`, Flyway `validate`, `ddl-auto=none`, `orders.scheduler.interval-ms=300000`.
- `test` (`application-test.yml`): H2 at `jdbc:h2:mem:orders;MODE=PostgreSQL`, same Flyway migrations, scheduler interval 200ms.

## 4. REST API

All paths under `/api/v1`. JSON in/out.

### 4.1 Inventory endpoints

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/products` | `{ name, sku, availableStock, unitPrice }` | 201 + `Product` | 409 duplicate sku |
| GET | `/products` | — | 200 `Product[]` | — |
| GET | `/products/{id}` | — | 200 `Product` | 404 |
| PUT | `/products/{id}` | `{ name, availableStock, unitPrice }` | 200 `Product` | 404, 409 version conflict |
| DELETE | `/products/{id}` | — | 204 | 404, 409 if any PENDING/PROCESSING order references it |

`DELETE` is blocked when there are active reservations on the product (PENDING or PROCESSING orders referencing it). Historical (SHIPPED/DELIVERED/CANCELLED) references do not block deletion.

### 4.2 Order endpoints

| Method | Path | Body | Success | Errors |
|---|---|---|---|---|
| POST | `/orders` | `{ customerName, items:[{ productSku, quantity }] }` | 201 + `Order` | 400 empty items / qty ≤ 0, 404 unknown sku, 409 insufficient stock |
| GET | `/orders/{id}` | — | 200 `Order` | 404 |
| GET | `/orders` | `?status=PENDING` (optional) | 200 `Order[]` | 400 unknown status value |
| PUT | `/orders/{id}/status` | `{ status }` ∈ {PROCESSING, SHIPPED, DELIVERED} | 200 `Order` | 400 invalid transition, 404, 409 version conflict |
| POST | `/orders/{id}/cancel` | — | 200 `Order` | 404, 409 if status ≠ PENDING |

### 4.3 Status transition rules (single source: `OrderStatus.canTransitionTo`)

- `PUT /orders/{id}/status` rejects `status=CANCELLED` and `status=PENDING`. Cancellation must use `POST /cancel`; there is no path back to `PENDING`.
- `POST /orders/{id}/cancel` enforces the PENDING-only business rule in the **service layer** (not the controller). Returns 409 when the order is not PENDING.

### 4.4 Stock coupling

All stock changes happen inside `OrderService` transactions:

- **`POST /orders`** calls `InventoryService.reserve(sku, qty)` for each line: decrement `Product.availableStock`, increment `Product.reservedStock`. Whole order creation is one `@Transactional` method; any line that can't be satisfied rolls back the whole order and returns 409.
- **`POST /orders/{id}/cancel`** (only when PENDING), per order item: `availableStock += qty; reservedStock -= qty`.
- **`PUT /orders/{id}/status` with `SHIPPED`** finalizes: `reservedStock -= qty` per item of that order. `availableStock` is not touched (units already left the sellable pool at reservation time).
- All three happen inside the same transaction as their respective status/order mutation.

### 4.5 Snapshotting

- `OrderItem.unitPrice` and `OrderItem.productName` are copied from `Product` at create time so product edits do not alter historical orders.
- `GET /products` shows live values; `GET /orders/{id}` shows snapshots.

## 5. Background Scheduler

**Class:** `com.peerislands.orders.orders.scheduler.PendingOrderScheduler` — a `@Component` with one `@Scheduled` method.

```
@Scheduled(fixedDelayString = "${orders.scheduler.interval-ms:300000}")
public void advancePendingOrders() { ... }
```

- `fixedDelayString` (not `fixedRateString`): measures end of one run to start of the next. Prevents overlapping ticks. Default 300000ms (5 min) per spec.
- Property `orders.scheduler.interval-ms` is configurable per profile. Default 300000 in main config; overridden to 200 in test config so tests can observe transitions without waiting 5 minutes. **The spec stays literal (5 min) in prod.**

### 5.1 Method body

1. `List<UUID> ids = orderRepository.findIdsByStatus(OrderStatus.PENDING)` (single SQL, indexed by `status`).
2. For each id: `try { orderService.transitionTo(id, PROCESSING); } catch (Exception e) { log.warn("skip {}", id); }`
3. The outer method is **not** `@Transactional`. No long-held transaction across all orders; no inflated persistence context.

### 5.2 `OrderService.transitionTo(UUID id, OrderStatus target)`

```
@Transactional(propagation = REQUIRES_NEW)
public Order transitionTo(UUID id, OrderStatus target) {
    Order o = orderRepository.findById(id).orElseThrow();   // fresh read in THIS tx
    o.transitionTo(target);                                  // status rule check
    return orderRepository.save(o);                          // version check here
}
```

Same method used by the manual PUT endpoint. **No back-door**: the scheduler cannot bypass transition rules, optimistic locking, or stock side-effects. It is just another caller.

### 5.3 Why REQUIRES_NEW + per-order try/catch

- Each order's update is its own transaction. If order #3 loses a version race, orders #1 and #2 already committed as PROCESSING and stay that way. We don't roll back good work.
- Per-order try/catch ensures one failing order doesn't abort the remaining iterations in the tick.
- Outer method being non-transactional avoids holding row locks or an inflated persistence context across N orders.

### 5.4 Race: cancel vs. scheduler

Concurrent `POST /orders/{id}/cancel` and scheduler tick on the same PENDING order `O` at version `v`.

**Case A — scheduler commits first:**
1. Scheduler loads `O` (v=v), sets `PROCESSING`, save → row updated, version `v+1`. Stock unchanged (PENDING→PROCESSING doesn't touch stock).
2. Cancel loads `O` (v=v), passes service-layer "is PENDING?" read-time check, sets `CANCELLED`, releases stock, save → `UPDATE ... WHERE version=v` → 0 rows → `OptimisticLockingFailureException`.
3. Mapped to **409 Conflict** for the client. Stock untouched. Consistent.

**Case B — cancel commits first:**
1. Cancel validates PENDING at read-time, sets `CANCELLED`, releases stock, version `v+1`.
2. Scheduler's `findIdsByStatus(PENDING)` ran before cancel committed, so `O`'s id is in its list. Scheduler calls `transitionTo → re-loads O by id inside the new tx`. Two safe sub-cases:
   - (i) Cancel committed before the re-load: re-load sees `CANCELLED`. `CANCELLED → PROCESSING` is not a valid transition → `IllegalOrderTransitionException` → scheduler catches per-order, logs WARN, skips.
   - (ii) Cancel commits after the re-load but before the save: scheduler's save hits version check → `OptimisticLockingFailureException` → same skip path.
3. Stock already released by cancel; scheduler never touched it. Consistent.

**Net:** optimistic locking + PENDING-only rule make the two operations mutually exclusive at commit time. Stock is reconciled exactly once.

### 5.5 What the scheduler does NOT do

- No `PROCESSING → SHIPPED` auto-advance.
- No retry logic, no queue, no "last run" metadata persistence. Idempotent: query PENDING, advance. Crashes mid-tick are recovered naturally on the next tick.
- No distributed lock. Single-instance assumption.
- No custom metrics. INFO log per tick: `"scheduler: advanced N pending orders in Xms"`.

## 6. Testing Strategy

**Stack:** JUnit 5, Spring Boot Test, MockMvc, H2 (`application-test.yml`), Flyway migrations run against H2 (so prod-Postgres and test-H2 share identical DDL), Awaitility.

### 6.1 Unit tests (plain JUnit, no Spring, no DB)

- `OrderStatusTest` — `canTransitionTo()` covers every legal/illegal pair; especially `PENDING → PROCESSING`, `PROCESSING → SHIPPED`, `SHIPPED → DELIVERED`, `PENDING → CANCELLED`, and rejected pairs (`SHIPPED → PENDING`, `DELIVERED → CANCELLED`, etc).
- `OrderTest` — `order.transitionTo(target)` enforces pre-condition and throws `IllegalOrderTransitionException` on illegal moves; snapshot test: construct order, mutate `Product.unitPrice`/`name`, assert `OrderItem` snapshots unchanged.

Fast (ms), no context, no DB.

### 6.2 Repository tests (`@DataJpaTest`) against H2

- `ProductRepositoryTest` — save, find by sku (unique), `@Version` concurrent-modification behavior → `OptimisticLockingFailureException`.
- `OrderRepositoryTest` — save with items, `findByStatus` / `findIdsByStatus`, `@Version` on Order, cascade persist/delete of items.
- Confirm Flyway migrations surface `version` column and `status` index correctly.

### 6.3 Integration / HTTP tests (`@SpringBootTest` + MockMvc)

**Inventory:** create, list, get by id, update (incl. version-conflict → 409), delete blocked when an active reservation exists.

**Order create:**
- happy path → 201, stock decremented (`availableStock`), `reservedStock` incremented.
- unknown sku → 404.
- insufficient stock → 409, no stock mutation (verify by re-reading product).
- empty items list → 400.
- snapshot: create order, change product price via PUT, GET order → order item still shows original values.

**Order retrieval:**
- GET by id → 200; unknown id → 404.
- GET all; filter `?status=PROCESSING` returns only processing.
- `?status=GARBLED` → 400.

**Status transitions (PUT endpoint):**
- `PENDING → DELIVERED` → 400 (skip not allowed).
- `PROCESSING → SHIPPED` → 200, `reservedStock` decremented per item.
- `SHIPPED → DELIVERED` → 200.
- `SHIPPED → PENDING` → 400.
- `PUT status=CANCELLED` → 400 (must use cancel endpoint).
- 404 on unknown id.
- Version conflict: two PUTs from the same loaded version → first 200, second 409.

**Cancel (POST /cancel):**
- from PENDING → 200, status `CANCELLED`, stock released (`availableStock += qty`, `reservedStock -= qty`).
- from PROCESSING → 409 ("can only cancel PENDING orders").
- 404 on unknown id.

**Scheduler test** (`PendingOrderSchedulerTest`, supplementary dep `org.awaitility:awaitility`):
- `application-test.yml` sets `orders.scheduler.interval-ms=200`.
- **Case A — happy:** seed 3 PENDING orders, await until 0 PENDING; assert all three PROCESSING; assert stock buckets unchanged (PENDING→PROCESSING doesn't touch stock).
- **Case B — cancel race (concrete):** seed 1 PENDING order. Immediately `POST /orders/{id}/cancel`. Both the scheduler (200ms ticks) and cancel race. Assert the invariant: after both commit, exactly one of:
  - order is CANCELLED and stock released (cancel won), OR
  - order is PROCESSING and stock still reserved (scheduler won; cancel got 409).
  In both branches: order is never both CANCELLED and PROCESSING; stock is released exactly once. Wait for "0 PENDING orders" via `await().atMost(2, SECONDS).until(...)`.
- **Case C — partial-failure isolation:** seed 5 PENDING orders; concurrently cancel order #3 mid-tick. After tick (`await until 0 PENDING`): assert ≤4 PROCESSING and 1 CANCELLED; no order is both; no order stuck PENDING unless version-race left one pending for the next tick (test tolerates this by re-awaiting, bounded by timeout).

### 6.4 Test data cleanliness

- Scheduler test methods are **non-transactional** (`REQUIRES_NEW` propagation would break under an outer test tx). State asserted via repository reads. `@DirtiesContext(classMode = AFTER_EACH_TEST)` on scheduler test class only.
- All other tests use `@Transactional` + default rollback for isolation.

### 6.5 Test directory layout (mirrors src/main)

```
src/test/java/com/peerislands/orders/
├── common/           GlobalExceptionHandlerTest
├── inventory/        ProductRepositoryTest, ProductServiceTest, ProductControllerTest
├── orders/           OrderServiceTest, OrderControllerTest, OrderStatusTest, OrderTest
│   └── scheduler/    PendingOrderSchedulerTest  (non-transactional, Awaitility)
```

### 6.6 Out of test scope

- No performance/load tests.
- No security/authz tests.
- No actual Postgres run (H2 chosen; we accept the small H2-vs-Postgres risk).
- No dialect-specific Flyway scripts — migrations are ANSI/Postgres-and-H2-compatible.

## 7. Build, Dev & Run

**Build:** Gradle (Kotlin DSL, `build.gradle.kts`), Java 17, Spring Boot 3.3.x.

**Dependencies (only what we use):**
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator` (health endpoint only)
- `flyway-core` + `postgresql` driver (runtime)
- `h2` (test scope)
- Test: `spring-boot-starter-test` (JUnit 5, MockMvc, AssertJ), `awaitility`
- No Lombok.

**Flyway migrations under `src/main/resources/db/migration`:**
- `V1__create_products.sql` — `products` table, `version BIGINT NOT NULL`, unique on `sku`.
- `V2__create_orders.sql` — `orders` (incl `version`, index on `status`) + `order_items` (FKs to `orders` + `products`).
- Dialect-agnostic SQL (`BIGINT`, `VARCHAR`, `NUMERIC`, `TIMESTAMP`) so H2 runs the same scripts.

**DTO validation:** Jakarta Bean Validation on DTOs (`@NotBlank`, `@NotNull`, `@Min(1)`, `@NotEmpty` on items list). `@Valid` on controller params → 400 on violation.

**How to run:**

```
git clone <repo> && cd orders
docker compose up -d         # postgres:16 with named volume
./gradlew bootRun
./gradlew test               # H2-backed tests
```

`docker-compose.yml` at repo root: one `postgres:16` service, named volume, env vars for `POSTGRES_DB=orders`, `POSTGRES_PASSWORD=postgres`, port 5432.

**Repo root files:**
- `build.gradle.kts`, `settings.gradle.kts`, `gradle/` (wrapper)
- `src/main/java/...`, `src/main/resources/`, `src/test/...`
- `docker-compose.yml`, `.gitignore`, `README.md`
- `docs/superpowers/specs/2026-07-06-order-processing-system-design.md` (this file)
- No CI config.

## 8. Constraints Captured

These are non-negotiable implementation rules derived from the design discussion:

1. `OrderService.transitionTo(UUID id, OrderStatus target)` re-loads the order by id inside its own `@Transactional(REQUIRES_NEW)` method. Never pass stale entities from the scheduler query into a new tx.
2. `@Version` (long) on `Order` and `Product`; `OptimisticLockingFailureException` → 409 in `GlobalExceptionHandler`.
3. Scheduler's outer method is **not** `@Transactional`. Each order's update is its own tx. Per-order try/catch.
4. Cancel endpoint enforces PENDING-only at the service layer, not the controller.
5. `PUT /orders/{id}/status` rejects `CANCELLED` and `PENDING`. Cancellation must go through `POST /cancel`.
6. `OrderItem.unitPrice` and `OrderItem.productName` are snapshots; never recomputed from `Product`.
7. Stock side-effects (reserve/release/finalize) always run inside the same transaction as the order/status mutation.
8. `DELETE /products/{id}` is blocked only when PENDING/PROCESSING orders reference the product.
9. Flyway migrations run on both Postgres and H2; no dialect-specific SQL.
10. Scheduler interval externalized as `orders.scheduler.interval-ms` (default 300000, test 200).