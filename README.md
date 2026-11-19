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

- API info: `GET http://localhost:8080/api/v1`
- Health: `GET http://localhost:8080/actuator/health`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Run the test suite with:

```shell
./mvnw test
```

Select another Spring profile with `-Dspring-boot.run.profiles=<profile>`.
