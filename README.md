# Orders

E-commerce Order Processing System — Spring Boot 3.3.x / Java 17.

## Build & run

```
docker compose up -d
./gradlew bootRun
./gradlew test
```

Health: `curl -s localhost:8080/actuator/health`
API base: `http://localhost:8080/api/v1`

See `docs/superpowers/specs/2026-07-06-order-processing-system-design.md` for the design.
