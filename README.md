# ARUS AI

Local-first intelligent payment operations API.

## Run the operations UI

Run both the Spring Boot API and Angular UI from the repository root:

```shell
./demo.sh
```

Open `http://localhost:4200`. The script installs frontend dependencies on first run, starts the API at `http://localhost:8080`, and stops both processes when you press Ctrl+C. The development proxy forwards `/api` requests to the API. On the sign-in screen, choose a demo username and one or more local roles. The UI confirms the selected identity with `GET /api/v1/session` and sends the mock identity headers with API requests. This is a local demonstration flow, not production authentication. No Docker or external service is required.

The UI includes an operations dashboard, payment record creation/listing, reconciliation runs and results, exception investigation and workflow actions, and approval review for authorized demo roles.

## Requirements

- Java 25
- No Docker or external infrastructure is required for the default local profile.

## Run locally

```shell
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` using the `local` profile by default.
The local profile uses a file-backed H2 database at `./data/arus-ai` and seeds 11 synthetic internal/external payment records (6 internal and 5 external) when no demo records exist. They include matched, discrepant and unmatched examples. Schema changes are applied with Flyway.

## Load and reset local sample data

The 11 synthetic sample payments are loaded automatically when the local database starts with no demo records. They are marked as demo records; subsequent starts preserve them and any records you added. For a completely fresh local database, stop the API first and remove the H2 database file:

```shell
rm ./data/arus-ai.mv.db
```

The next start recreates the schema through Flyway and reloads the 11 sample payments. This full reset permanently deletes all local payment and operational data in that database; back up the file first if needed. Do not remove it while the API is running.

Additional sample payments can be created with `POST /api/v1/payments` or loaded in batches with `POST /api/v1/payments/import` using the JSON or CSV format described below. Each import is limited to 10,000 rows and requires an `Idempotency-Key`.

## Optional PostgreSQL runtime

The default `./demo.sh` runtime remains local and uses H2. To use an already running PostgreSQL instance, activate the opt-in Maven and Spring profiles:

```shell
ARUS_DATABASE_URL=jdbc:postgresql://localhost:5432/arus_ai \
ARUS_DATABASE_USERNAME=arus \
ARUS_DATABASE_PASSWORD='your-local-password' \
./mvnw -Ppostgres -Dspring-boot.run.profiles=local,postgres spring-boot:run
```

The `postgres` Maven profile adds the JDBC driver and PostgreSQL Flyway module; `application-postgres.yml` supplies the database configuration. No Docker or database service is installed or started by this command. The combined profile keeps local mock authentication enabled for development only. Kafka, Redis, Keycloak, pgvector and external AI provider connectors are not active yet; their adapter boundaries and integration requirements are documented in [Optional Integrations](docs/INTEGRATIONS.md).

- API info: `GET http://localhost:8080/api/v1`
- Payment records: `GET/POST http://localhost:8080/api/v1/payments`, `GET /api/v1/payments/{id}`
- Batch payment import: `POST http://localhost:8080/api/v1/payments/import` (JSON or CSV)
- Reconciliation: `POST http://localhost:8080/api/v1/reconciliation/batches`, then fetch the batch and its results
- Exceptions: `GET http://localhost:8080/api/v1/exceptions`
- Approvals: `GET http://localhost:8080/api/v1/approvals/pending`
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

Run reconciliation against all currently stored payment records. Matching is deterministic and one-to-one: exact transaction reference first, then a unique end-to-end ID, then a unique exact amount/currency/value-date/counterparty combination. Amount, currency, date, counterparty, or failed/rejected status differences on an identifier match produce a `DISCREPANCY`; records without a unique counterpart produce `UNMATCHED`. Matching evidence and batch summaries are persisted.

```shell
curl -X POST http://localhost:8080/api/v1/reconciliation/batches
curl http://localhost:8080/api/v1/reconciliation/batches/<batch-id>
curl http://localhost:8080/api/v1/reconciliation/batches/<batch-id>/results
curl http://localhost:8080/api/v1/reconciliation/batches
```

Network submissions return `202 Accepted` with a submission ID and `PENDING` status. The local Spring event listener processes the request asynchronously; poll its status endpoint for `SUCCEEDED`, `REJECTED`, or `FAILED`. The simulator deterministically accepts normal records and rejects records whose payment status is `FAILED` or `REJECTED`. Submission status is cached in memory for up to 30 minutes.

Reconciliation automatically creates an exception for each discrepancy or unmatched result. Exceptions can be assigned, prioritized by severity, investigated with append-only notes, and reviewed through their audit history. Resolution and closure require a separate approval: an exception must be investigated with at least one note before requesting resolution; a resolved case requires a distinct approval before closure. The requester cannot approve their own request. Approval and rejection decisions require a justification.

Repeated reconciliation runs do not create another exception for a payment record that already has an exception.

The local investigation AI is a deterministic fixture service behind the `InvestigationAiService` interface; it does not call an LLM or execute recommendations. For an exception, request `GET /api/v1/exceptions/{exceptionId}/investigation-report` using an authorized operations role. The report includes a category-specific fixture ID, confidence score and label, recommendations with their own confidence indicators, and typed evidence references to the exception, reconciliation result, payment records, and any investigation notes. Confidence values are fixed fixture indicators for demonstration, not calibrated model probabilities; recommendations are advisory and require human review.

The local mock identity defaults to `demo-operator` with `PAYMENT_OPERATOR`. Override it using `X-Mock-User` and `X-Mock-Roles` headers, for example `X-Mock-Roles: OPERATIONS_MANAGER,RECONCILIATION_OFFICER`. Reconciliation runs require `RECONCILIATION_OFFICER` or `OPERATIONS_MANAGER`; assignment requires `RECONCILIATION_OFFICER`, `OPERATIONS_MANAGER`, or `PAYMENT_ADMIN`; severity changes require `OPERATIONS_MANAGER`, `COMPLIANCE_OFFICER`, or `PAYMENT_ADMIN`; approvals require `OPERATIONS_MANAGER` or `COMPLIANCE_OFFICER`. Only roles defined by the application are accepted. These headers are development-only and must not be used as authentication in a deployed environment.

Workflow endpoints include `PUT /api/v1/exceptions/{id}/assignment`, `PUT /api/v1/exceptions/{id}/severity`, `POST` and `GET /api/v1/exceptions/{id}/notes`, `POST /api/v1/exceptions/{id}/resolution-requests`, `GET /api/v1/approvals/pending`, `POST /api/v1/approvals/exception-resolution-requests/{requestId}/decision`, and `GET /api/v1/exceptions/{id}/audit`. Resolution requests accept `{"action":"RESOLVE","justification":"..."}` or `{"action":"CLOSE","justification":"..."}`; decisions accept `{"decision":"APPROVED","justification":"..."}` or `REJECTED`.

```shell
curl -X POST http://localhost:8080/api/v1/payments/<payment-id>/network-submissions
curl http://localhost:8080/api/v1/network-submissions/<submission-id>
curl -H 'X-Mock-User: demo-manager' \
  -H 'X-Mock-Roles: OPERATIONS_MANAGER,RECONCILIATION_OFFICER' \
  http://localhost:8080/api/v1/session

curl -H 'X-Mock-Roles: RECONCILIATION_OFFICER' \
  -X POST http://localhost:8080/api/v1/reconciliation/batches
curl -H 'X-Mock-Roles: OPERATIONS_MANAGER' \
  -H 'Content-Type: application/json' \
  -X PUT http://localhost:8080/api/v1/exceptions/<exception-id>/assignment \
  -d '{"assignedTo":"demo-operator"}'
curl -H 'X-Mock-Roles: PAYMENT_OPERATOR' \
  -H 'Content-Type: application/json' \
  -X POST http://localhost:8080/api/v1/exceptions/<exception-id>/notes \
  -d '{"note":"Reviewed network response and confirmed the mismatch."}'
curl -H 'X-Mock-Roles: PAYMENT_OPERATOR' \
  http://localhost:8080/api/v1/exceptions/<exception-id>/investigation-report
curl -H 'X-Mock-Roles: OPERATIONS_MANAGER' \
  http://localhost:8080/api/v1/approvals/pending
```

By default, existing demo records are preserved. Set `--arus.demo-data.enabled=false` to disable automatic seeding.

Run the test suite with:

```shell
./mvnw test
```

The test suite includes unit tests for deterministic matching, API and workflow integration tests, and authorization checks. Tests use isolated in-memory H2 databases. To exercise the database-backed 100,000-payment reconciliation integration test separately:

```shell
./mvnw -Dtest=ReconciliationScaleIT test
```

The scale test inserts 100,000 synthetic persisted payment records (50,000 matched pairs) into an isolated in-memory H2 database and verifies all 50,000 reconciliation results were persisted. A run in this development environment completed reconciliation and result persistence in about 1.7 seconds; dataset setup is outside the timed interval, and this measurement is not a production SLA. It is an opt-in test because it exercises a larger workload than the normal suite. Reconciliation processes all payment records currently stored, not just records from the most recent import.

Select another Spring profile with `-Dspring-boot.run.profiles=<profile>`. Tests use isolated in-memory H2 databases.
