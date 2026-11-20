# Optional Integrations

The default runtime is the `local` Spring profile. It uses H2, Spring application events, a bounded
Caffeine cache, local mock identity, the payment-network simulator and deterministic fixture AI. It
does not require Docker, credentials, or external services.

The integration ports are intended to keep those local implementations replaceable:

| Concern | Application boundary | Local implementation | Optional integration status |
|---|---|---|---|
| Operational database | Spring Data JPA repositories | H2 | PostgreSQL has an opt-in Maven and Spring profile |
| Domain events | `DomainEventPublisher` | Spring application events | Kafka publisher/consumer adapter is deferred |
| Payment network | `PaymentNetworkAdapter` | `LocalPaymentNetworkSimulator` | Provider adapter is deferred |
| Submission status cache | `PaymentNetworkSubmissionStore` | Caffeine-backed `PaymentNetworkSubmissionCache` | Redis-backed store is deferred |
| Authentication | Spring Security filter chain | Local mock identity | Keycloak/OIDC resource-server configuration is deferred |
| Investigation AI | `InvestigationAiService` | `MockInvestigationAiService` | External provider adapters are deferred |
| Vector retrieval | Not currently used by application features | None | pgvector adapter and schema are deferred until a retrieval feature is implemented |

`DomainEventPublisher`, `PaymentNetworkSubmissionStore`, `PaymentNetworkAdapter`, and
`InvestigationAiService` are application-owned boundaries. The local implementations are limited to
the `local` and `test` profiles where appropriate. A new infrastructure implementation should be
selected explicitly by its own Spring profile, preserve transaction/authorization semantics, and
have contract tests that also run against the local implementation.

## PostgreSQL profile

The PostgreSQL JDBC driver and Flyway database module are isolated in the Maven `postgres` profile.
Use an already running PostgreSQL instance; this project does not start or provision one:

```shell
ARUS_DATABASE_URL=jdbc:postgresql://localhost:5432/arus_ai \
ARUS_DATABASE_USERNAME=arus \
ARUS_DATABASE_PASSWORD='your-local-password' \
./mvnw -Ppostgres -Dspring-boot.run.profiles=local,postgres spring-boot:run
```

`application-postgres.yml` supplies local-development defaults for the URL and username. Set the
password through the environment. The `local` profile is deliberately included above for local
mock authentication and sample data; do not use that profile combination as a deployed security
configuration.

## Planned integration profiles

Kafka, Redis, Keycloak, pgvector, and external AI provider profiles are not implemented or activated
by this change. Their connection properties, dependencies and adapters must be added together when
each integration is built; adding credentials or profile names alone must not silently switch
application behavior.

- Kafka must publish domain events only after the originating database transaction commits, and
  consumers must be idempotent.
- Redis must preserve the submission-status TTL/bounds contract and define behavior after cache
  eviction or restart.
- Keycloak must replace, not combine with, client-controlled mock role headers. Role mapping and
  endpoint authorization need integration tests.
- pgvector requires an explicit embedding model, schema/migrations, and retrieval authorization
  before a vector-search adapter is useful.
- External AI adapters must implement `InvestigationAiService`, validate provider responses, avoid
  automatic financial actions, and remain opt-in with credentials supplied from the environment.

Use the local profile for the normal demo and tests. Optional integration profiles must never be
included in `demo.sh`.
