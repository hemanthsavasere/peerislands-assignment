# Orders

E-commerce order processing system handling inventory management, order lifecycle, and concurrent stock reservation — built with Spring Boot 3.3 and Java 17.

A REST API that lets you manage products, create orders, and move them through a defined state machine (PENDING → PROCESSING → SHIPPED → DELIVERED, with cancellation). Every order creation reserves stock and snapshots pricing. A background scheduler automatically advances pending orders. Concurrency between order state transitions, stock mutations, and the scheduler is handled through [JPA optimistic locking](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic) — no distributed locks, no queues.

## Techniques

- **[Optimistic Locking](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic) via `@Version`** — Both `Order` and `Product` entities use versioned rows. Concurrent modifications detect conflicts at commit time and return HTTP 409 rather than silently overwriting. The scheduler-vs-cancel race is resolved purely through these version checks.
- **[State Machine in an Enum](https://docs.oracle.com/javase/tutorial/java/javaOO/enum.html)** — `OrderStatus` encodes the full transition graph (`canTransitionTo()`). The order entity itself (`Order.transitionTo()`) rejects invalid moves, keeping domain logic in the model rather than scattered across services.
- **[Database Migrations with Flyway](https://flywaydb.org/documentation/)** — Schema is versioned through [Flyway](https://flywaydb.org/) SQL migrations, not Hibernate DDL generation. `ddl-auto` is set to `none`. Migrations are dialect-agnostic, valid against both PostgreSQL and H2.
- **[Dependency Inversion Bridge](https://en.wikipedia.org/wiki/Dependency_inversion_principle)** — The `ReservationLookup` interface lives in the `inventory` package, but its implementation (`OrderItemReservationLookup`) lives in `orders`. This keeps the inventory service from directly depending on order repositories — a clean cross-package boundary without a shared kernel.
- **[Snapshot Pattern](https://martinfowler.com/eaaCatalog/snapshot.html)** — `OrderItem` copies `productName` and `unitPrice` from `Product` at creation time. Edits to a product after an order is placed don't retroactively change historical order data. Verified by `orderSnapshotSurvivesProductEdit` in the test suite.
- **[Per-Record Scheduler Transactions](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)** — The `PendingOrderScheduler` outer method is not transactional. Each order transition gets its own transaction with try/catch isolation, so a failure on one order doesn't abort the entire batch. Uses `fixedDelayString` (not `fixedRateString`) so scheduler ticks never overlap.
- **[`@RestControllerAdvice` Exception Mapping](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)** — Seven exception types map to specific HTTP status codes with a consistent `ApiError` JSON body. A `@Profile("test")` probe controller lets the test suite exercise every exception path without needing side-effectful business logic.
- **[Two-Counter Stock Model](https://martinfowler.com/bliki/TwoHardThings.html)** — `availableStock` and `reservedStock` are tracked separately. `reserve()` moves from available to reserved; `release()` reverses it; `finalizeReservation()` removes from reserved only. This avoids phantom inventory during the PENDING→PROCESSING window.

## Non-Obvious Tools and Libraries

- **[H2 in PostgreSQL Compatibility Mode](https://h2database.com/html/features.html#compatibility)** — All tests run against H2 (configured with `MODE=PostgreSQL`) instead of Testcontainers. Faster test startup, no Docker dependency in CI, and the dialect-agnostic Flyway SQL works on both engines.
- **[Awaitility](https://github.com/awaitility/awaitility)** (4.2.2) — Used exclusively in scheduler integration tests. Instead of `Thread.sleep()` or polling loops, `await().untilAsserted(...)` waits for async scheduler ticks with a timeout. The test profile drops the scheduler interval from 5 minutes to 200ms.
- **[Flyway](https://flywaydb.org/)** — Schema versioning and migration (chosen over Liquibase). Migrations live in [`src/main/resources/db/migration/`](src/main/resources/db/migration/).
- **No Lombok** — All getters, setters, constructors are explicit. No MapStruct, no code generation. Keeps the build surface small and the code transparent for review.
- **No distributed scheduler locking** — The design assumes a single application instance. For horizontal scaling, you'd need [ShedLock](https://github.com/lukas-krecan/ShedLock) or a similar distributed lock.

## Project Structure

```
.
├── docker-compose.yml          # PostgreSQL 16 with persistent volume
├── build.gradle.kts            # Spring Boot 3.3.5, Java 17, Flyway, H2
├── settings.gradle.kts
├── gradlew / gradlew.bat
└── src/
    ├── main/
    │   ├── java/com/peerislands/orders/
    │   │   ├── OrdersApplication.java
    │   │   ├── common/          # Shared: exceptions, DTOs, exception handler, test probe
    │   │   ├── inventory/       # Product CRUD, stock reservation/release/finalize
    │   │   └── orders/          # Order CRUD, state machine, scheduler, DTOs
    │   └── resources/
    │       ├── db/migration/    # Flyway SQL migrations (V1, V2)
    │       ├── application.yml       # Production config (PostgreSQL)
    │       └── application-test.yml  # Test config (H2, fast scheduler)
    └── test/
        └── java/com/peerislands/orders/
            ├── common/          # Exception handler tests
            ├── inventory/       # Product unit, repository, service, and controller tests
            └── orders/          # Order unit, repository, service, controller, and scheduler tests
```

- [`common/`](src/main/java/com/peerislands/orders/common/) — Shared exceptions mapped to HTTP status codes, the global `@RestControllerAdvice`, and a test-only probe controller.
- [`inventory/`](src/main/java/com/peerislands/orders/inventory/) — Product entity with optimistic locking, stock reserve/release/finalize operations, and the `ReservationLookup` interface that bridges to the orders package.
- [`orders/`](src/main/java/com/peerislands/orders/orders/) — Order and OrderItem entities, the state machine enum, the REST controller, and the service that orchestrates stock reservation during order creation.
- [`orders/scheduler/`](src/main/java/com/peerislands/orders/orders/scheduler/) — Background component that advances PENDING orders to PROCESSING with per-order transaction boundaries.
- [`src/test/`](src/test/) — Layered tests: plain JUnit for domain logic, `@DataJpaTest` for repositories, `@SpringBootTest` with MockMvc for controller and service integration, and Awaitility for scheduler tests.
