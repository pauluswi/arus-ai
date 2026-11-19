# ARUS AI

Local-first intelligent payment operations API.

## Requirements

- Java 25
- No Docker or external infrastructure is required for the default local profile.

## Run locally

```shell
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` using the `local` profile by default.
The local profile uses a file-backed H2 database at `./data/arus-ai` and seeds 11 synthetic internal/external payment records when no demo records exist. Schema changes are applied with Flyway.

- API info: `GET http://localhost:8080/api/v1`
- Health: `GET http://localhost:8080/actuator/health`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/arus-ai`, user `sa`, blank password)

To replace only the records marked as synthetic demo data on startup (other records are preserved):

```shell
./mvnw spring-boot:run -Dspring-boot.run.arguments=--arus.demo-data.reset=true
```

By default, existing demo records are preserved. Set `--arus.demo-data.enabled=false` to disable automatic seeding.

Run the test suite with:

```shell
./mvnw test
```

Select another Spring profile with `-Dspring-boot.run.profiles=<profile>`. Tests use an isolated in-memory H2 database.
