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
- Payment records: `GET/POST http://localhost:8080/api/v1/payments`, `GET /api/v1/payments/{id}`
- Batch payment import: `POST http://localhost:8080/api/v1/payments/import` (JSON or CSV)
- Mock identity: `GET http://localhost:8080/api/v1/session`
- Payment network simulation: `POST /api/v1/payments/{id}/network-submissions`, then poll `GET /api/v1/network-submissions/{submissionId}`
- Health: `GET http://localhost:8080/actuator/health`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/arus-ai`, user `sa`, blank password)

Create an internal or external payment record with a JSON request:

```shell
curl -X POST http://localhost:8080/api/v1/payments \
  -H 'Content-Type: application/json' \
  -d '{
    "source": "INTERNAL",
    "transactionReference": "INT-2001",
    "endToEndId": "E2E-2001",
    "amount": 125.50,
    "currency": "EUR",
    "counterparty": "Example GmbH",
    "valueDate": "2026-11-18",
    "status": "RECEIVED"
  }'
```

Amounts must be positive with up to 15 integer and 4 fractional digits. Currency is a three-letter uppercase code. Duplicate `(source, transactionReference)` pairs return `409 Conflict`.

Import multiple records as JSON or CSV. Both formats require a unique `Idempotency-Key` (up to 120 characters). The same key and payload replays the original response with `Idempotency-Replayed: true`; reusing a key with a different payload returns `409 Conflict`. Each response includes an outcome for every row: `CREATED` or `DUPLICATE`. The API accepts up to 10,000 rows per request; CSV bodies are limited to 5 MiB. Duplicate records already stored or repeated within an import are reported as `DUPLICATE` and are not inserted.

```shell
curl -X POST http://localhost:8080/api/v1/payments/import \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-import-001' \
  -d '{
    "records": [{
      "source": "EXTERNAL",
      "transactionReference": "NET-2001",
      "endToEndId": "E2E-2001",
      "amount": 125.50,
      "currency": "EUR",
      "counterparty": "Example GmbH",
      "valueDate": "2026-11-18",
      "status": "RECEIVED"
    }]
  }'

curl -X POST http://localhost:8080/api/v1/payments/import \
  -H 'Content-Type: text/csv' \
  -H 'Idempotency-Key: demo-csv-import-001' \
  --data-binary @payments.csv
```

CSV files must contain exactly these case-sensitive headers in any order: `source,transactionReference,endToEndId,amount,currency,counterparty,valueDate,status`.

Network submissions return `202 Accepted` with a submission ID and `PENDING` status. The local Spring event listener processes the request asynchronously; poll its status endpoint for `SUCCEEDED`, `REJECTED`, or `FAILED`. The simulator deterministically accepts normal records and rejects records whose payment status is `FAILED` or `REJECTED`. Submission status is cached in memory for up to 30 minutes.

The local mock identity defaults to `demo-operator` with `PAYMENT_OPERATOR`. Override it using `X-Mock-User` and `X-Mock-Roles` headers, for example `X-Mock-Roles: OPERATIONS_MANAGER,RECONCILIATION_OFFICER`. Only roles defined by the application are accepted. These headers are development-only and must not be used as authentication in a deployed environment.

```shell
curl -X POST http://localhost:8080/api/v1/payments/<payment-id>/network-submissions
curl http://localhost:8080/api/v1/network-submissions/<submission-id>
curl -H 'X-Mock-User: demo-manager' \
  -H 'X-Mock-Roles: OPERATIONS_MANAGER,RECONCILIATION_OFFICER' \
  http://localhost:8080/api/v1/session
```

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
