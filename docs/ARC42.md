# ARUS AI — arc42 Architecture Documentation

**Product:** ARUS AI
**Positioning:** Intelligent Payment Operations Platform
**Tagline:** Intelligence Behind Every Transaction.
**Category:** Enterprise Banking AI / Payment Technology
**Architecture:** Modular, Event-Driven, AI-Enabled
**AI Approach:** Hybrid AI + Multi-Agent Architecture.

---

# 1. Introduction and Goals

## 1.1 Purpose

This document describes the software architecture of ARUS AI, an intelligent payment operations platform designed to automate payment reconciliation, detect transaction exceptions, investigate payment failures and recommend corrective actions using Artificial Intelligence.

The architecture combines:

- Deterministic payment processing.
- Machine Learning.
- Generative AI.
- Retrieval-Augmented Generation (RAG).
- Agentic AI.
- Human-in-the-loop governance.

The purpose is to demonstrate how AI can be safely integrated into enterprise banking systems without compromising financial integrity, operational controls or auditability.

## 1.2 Business Context

Financial institutions process high volumes of payment transactions across internal banking systems, external payment networks and settlement institutions.

Traditional payment operations rely heavily on predefined matching rules, manual investigation and operational intervention.

ARUS AI introduces intelligent automation to reduce manual workloads and improve payment operational visibility.

### Business objectives

1. Automate payment reconciliation.
2. Improve transaction exception detection.
3. Accelerate payment investigation.
4. Recommend evidence-based resolution actions.
5. Provide operational intelligence.
6. Introduce controlled agentic AI capabilities.
7. Maintain transparency, security and financial accountability.

## 1.3 Quality Goals

| Priority | Quality Goal | Description |
|---|---|---|
| 1 | Financial Integrity | Protect transaction consistency and prevent unauthorized financial changes |
| 2 | Reliability | Ensure payment and reconciliation processes remain reliable |
| 3 | Explainability | Provide evidence supporting AI-generated recommendations |
| 4 | Security | Protect sensitive financial information and operational actions |
| 5 | Scalability | Support increasing transaction volumes |
| 6 | Maintainability | Maintain modular and independently testable components |
| 7 | Performance | Process large reconciliation batches efficiently |
| 8 | Auditability | Maintain traceable operational and AI decisions |

## 1.4 Stakeholders

| Stakeholder | Interest |
|---|---|
| Payment Operations | Efficient exception investigation and resolution |
| Reconciliation Team | Accurate transaction matching |
| Operations Management | Operational performance and SLA monitoring |
| Compliance | Financial controls and auditability |
| IT Architecture | Integration, scalability and maintainability |
| AI Governance | Model performance and responsible AI |
| Security Team | Data protection and authorization |
| Business Management | Operational efficiency and cost reduction |

---

# 2. Architecture Constraints

The architecture must comply with the following technical and business constraints.

## 2.1 Business Constraints

**BC-001 — Financial Integrity**

AI must never directly modify financial ledger balances.

**BC-002 — Human Oversight**

Sensitive financial actions must require appropriate authorization.

**BC-003 — Auditability**

All significant payment operations, AI recommendations, approvals and executions must be traceable.

**BC-004 — Payment Standards**

The initial implementation must support ISO 20022-inspired payment data structures and simulated SEPA payment scenarios.

**BC-005 — Synthetic Data**

The initial implementation must use synthetic transaction data and simulated external payment network integrations.

## 2.2 Technical Constraints

| Constraint | Technology / Decision |
|---|---|
| Frontend | Angular |
| Core Backend | Java 25, Spring Boot |
| Java API Build Tool | Apache Maven |
| AI Backend | Python, FastAPI |
| AI Orchestration | LangGraph |
| LLM | Gemini or OpenAI API |
| Machine Learning | scikit-learn, XGBoost |
| Local Demo Database | H2 or PostgreSQL-compatible mode |
| Vector Search | In-memory vector mock / optional pgvector |
| Messaging | Spring Events locally / optional Kafka |
| Cache | In-memory cache locally / optional Redis |
| Authentication | Mock OAuth 2.0 / OIDC provider locally |
| Containerization | Not required for local development |
| Orchestration | Not required for the initial implementation |
| Monitoring | Prometheus, Grafana |
| Tracing | OpenTelemetry |
| CI/CD | GitHub Actions |

Technology versions and compatibility must be validated before implementation.

## 2.3 Architectural Constraints

1. Core payment processing must remain independent of AI services.
2. AI workloads must be isolated from deterministic transaction processing.
3. All financial actions must pass through controlled backend services.
4. AI agents must access business capabilities through explicitly authorized tools.
5. Payment data must not be exposed to unauthorized AI services.
6. External LLM dependencies must not become mandatory dependencies for core reconciliation.
7. Financial records must be preserved with appropriate transaction and audit controls.

---

# 3. Context and Scope

## 3.1 Business Context

ARUS AI interacts with the following external actors and systems:

| External Actor / System | Interaction |
|---|---|
| Payment Operations Officer | Investigates and resolves exceptions |
| Reconciliation Officer | Reviews reconciliation results |
| Operations Manager | Monitors operational performance |
| Internal Banking System | Provides internal payment transactions |
| Payment Network Simulator | Provides external payment records and responses |
| LLM Provider | Provides generative AI capabilities |
| Knowledge Repository | Provides payment documentation and procedures |
| Identity Provider | Provides authentication and identity management |

## 3.2 System Context Diagram

```mermaid
flowchart TB
    OPS[Payment Operations Officer]
    REC[Reconciliation Officer]
    MGR[Operations Manager]

    ARUS[ARUS AI Platform]

    CORE[Internal Banking Simulator]
    NETWORK[Payment Network Simulator]
    LLM[LLM Provider]
    KB[Payment Knowledge Repository]
    IDP[Identity Provider]

    OPS --> ARUS
    REC --> ARUS
    MGR --> ARUS

    CORE --> ARUS
    NETWORK --> ARUS

    ARUS <--> LLM
    ARUS <--> KB
    ARUS <--> IDP
```

## 3.3 System Responsibilities

ARUS AI is responsible for:

- Payment transaction ingestion.
- Payment reconciliation.
- Exception identification.
- Investigation management.
- AI-assisted root-cause analysis.
- Resolution recommendations.
- Approval workflows.
- Operational dashboards.
- AI governance and audit.

External banking systems and payment networks remain responsible for their own transaction processing and settlement operations.

---

# 4. Solution Strategy

## 4.1 Overall Architecture Strategy

ARUS AI adopts a hybrid architecture combining deterministic business processing with probabilistic AI capabilities.

The architecture is divided into five logical layers.

### Layer 1 — Application Layer

Provides user-facing functionality:

- Payment Operations Dashboard.
- Reconciliation Workbench.
- Exception Investigation Workbench.
- AI Operations Copilot.

### Layer 2 — Business Services Layer

Implements deterministic banking capabilities:

- Payment Management.
- Reconciliation Engine.
- Exception Management.
- Resolution Workflow.
- Audit Management.

### Layer 3 — AI Intelligence Layer

Provides intelligent capabilities:

- Machine Learning.
- LLM Integration.
- RAG.
- Multi-Agent Orchestration.
- AI Evaluation.

### Layer 4 — Integration Layer

Provides communication with external systems:

- REST APIs.
- Spring Events and in-process event dispatching for local execution.
- Optional Kafka event adapter.
- Payment Network Adapters.
- Mock and optional external LLM Provider Adapters.
- Knowledge Repository Integration.

### Layer 5 — Infrastructure Layer

Provides:

- H2 or PostgreSQL.
- In-memory cache or optional Redis.
- In-process messaging or optional Kafka.
- In-memory vector storage or optional pgvector.
- Maven Spring Boot runtime.
- Monitoring and tracing.

## 4.2 Hybrid AI Strategy

### Local mock strategy

The default Spring profile must run the complete application with H2, Spring Events, mock authentication, the payment network simulator and deterministic mock AI responses.

FastAPI, external LLM providers, Kafka, Redis, pgvector and Keycloak are optional integrations. They must be behind adapters so switching implementations does not change core payment or reconciliation code.

The local payment-network adapter returns deterministic simulated acceptance or rejection responses. Network submissions are dispatched through an after-commit Spring event listener and their status is kept in a bounded, expiring Caffeine cache. Local mock identity headers expose recognized ARUS roles for development only; they are not a production authentication mechanism.

ARUS AI uses different AI techniques according to the nature of the business problem.

| Business Problem | Approach |
|---|---|
| Exact transaction matching | Deterministic rules |
| Ambiguous transaction matching | ML-assisted candidate ranking |
| Duplicate detection | Deterministic rules + ML |
| Transaction anomalies | Unsupervised ML |
| Exception classification | Supervised ML |
| Payment failure prediction | Supervised ML |
| Investigation explanation | LLM + RAG |
| Root-cause investigation | Agentic AI |
| Resolution recommendations | Agentic AI + business rules |
| Operations assistance | RAG + conversational AI |

### Core principle

**Deterministic systems make financial decisions. AI provides intelligence and recommendations. Authorized humans control sensitive actions.**

## 4.3 Multi-Agent Strategy

ARUS AI uses LangGraph to coordinate specialized agents.

The initial implementation includes five agents:

1. Reconciliation Agent.
2. Payment Investigation Agent.
3. Payment Repair Agent.
4. Operations Intelligence Agent.
5. Operations Copilot.

Agents must not communicate with banking infrastructure using unrestricted database access.

All agent capabilities must be exposed through controlled application APIs or explicitly authorized tools.

## 4.4 Integration Strategy

- Synchronous operations use REST APIs.
- Local asynchronous processing uses Spring Events.
- External deployments may use an optional Kafka adapter.
- AI workflows use LangGraph.
- External payment integrations use adapter interfaces.
- AI providers use an abstraction layer to support provider replacement.
- Java REST APIs are built, tested and packaged with Apache Maven.

## 4.5 Architectural Style

The initial release uses a **Modular Monolith for the core banking application**, with independently deployable Python AI services and infrastructure components.

This is preferable to introducing numerous microservices prematurely.

The design must preserve module boundaries so that individual components can be extracted into independent services when justified by scaling, ownership or operational requirements.

---

# 5. Building Block View

## 5.1 Level 1 — System Decomposition

The ARUS AI platform consists of the following major building blocks.

```mermaid
flowchart TB
    UI[Angular Frontend]

    subgraph CORE[Spring Boot Application]
        API[API Layer]
        PM[Payment Management]
        RE[Reconciliation Engine]
        EM[Exception Management]
        WF[Resolution Workflow]
        AUD[Audit Management]
    end

    subgraph AI[Python AI Platform]
        ORCH[Agent Orchestrator]
        ML[ML Services]
        RAG[RAG Services]
        AGENTS[Specialized Agents]
    end

    subgraph INFRA[Local Infrastructure / Optional Adapters]
        DB[(H2 / PostgreSQL)]
        VDB[(In-Memory / pgvector)]
        EVENTS[Spring Events / Kafka]
        CACHE[In-Memory / Redis]
    end

    LLM[External LLM Provider]

    UI --> API
    API --> PM
    API --> RE
    API --> EM
    API --> WF

    PM --> DB
    RE --> DB
    EM --> DB
    AUD --> DB

    CORE --> EVENTS
    EVENTS --> ORCH

    ORCH --> ML
    ORCH --> RAG
    ORCH --> AGENTS

    RAG --> VDB
    AGENTS --> LLM
    AI --> CORE
```

## 5.2 Level 2 — Core Application Modules

### 5.2.1 Payment Management Module

Responsibilities:

- Import internal payment transactions.
- Import external payment records.
- Maintain transaction lifecycle.
- Validate payment data.
- Prevent duplicate ingestion.
- Publish payment events.

The local batch-import endpoint accepts JSON records or CSV with the exact fields `source,transactionReference,endToEndId,amount,currency,counterparty,valueDate,status`. Requests require a persisted idempotency key (maximum 120 characters); matching retries replay their saved response, while changed payloads under the same key conflict. Each batch is capped at 10,000 records and CSV content at 5 MiB. Duplicate references are returned as per-row outcomes and excluded from persistence. A post-commit Spring event is published only when new records have been stored.

Key domain objects:

- PaymentTransaction.
- ExternalTransaction.
- PaymentNetwork.
- PaymentEvent.
- PaymentStatus.

### 5.2.2 Reconciliation Engine

Responsibilities:

- Execute matching rules.
- Perform exact transaction matching.
- Perform composite matching.
- Identify unmatched transactions.
- Detect duplicate records.
- Calculate reconciliation discrepancies.
- Generate reconciliation results.

Matching strategy:

1. Exact transaction reference matching.
2. End-to-end identifier matching.
3. Amount and currency matching.
4. Date and counterparty matching.
5. ML-assisted candidate ranking for ambiguous cases.

Financially significant matches must be confirmed by deterministic validation and applicable review policies.

The initial Java API implementation uses deterministic one-to-one matching in priority order: exact transaction reference, unique end-to-end identifier, then a unique exact amount/currency/value-date/counterparty key. Identifier matches with a different amount, currency, date, counterparty, or a failed/rejected payment status are persisted as discrepancies; unmatched records remain explicit results. Batch execution and matching evidence are persisted. Ambiguous candidates are not auto-paired, and ML ranking, one-to-many matching, tolerances, and exception generation remain later capabilities.

### 5.2.3 Exception Management Module

Responsibilities:

- Generate exception cases.
- Classify discrepancies.
- Assign severity.
- Manage investigation lifecycle.
- Assign cases to users.
- Track SLA.
- Record investigation outcomes.
- Preserve investigation notes and append-only audit history.
- Route sensitive resolution and closure actions through independent approval.

The Java API creates an exception for each unmatched or discrepant reconciliation result. Assignment is limited to reconciliation officers, operations managers, and payment administrators. Severity changes require an operations manager, compliance officer, or payment administrator. Operators and reconciliation officers can add investigation notes and request resolution; an operations manager or compliance officer must approve or reject. Self-approval is prohibited. A close request is permitted only after resolution and follows the same independent approval rule. The request, decision, actor, and justification are persisted; assignment, severity, notes, and lifecycle changes are audited.

Repeated reconciliation runs do not generate duplicate open exceptions for payment records with an existing exception.

### 5.2.4 Resolution Workflow Module

Responsibilities:

- Receive AI resolution recommendations.
- Validate proposed actions.
- Enforce authorization policies.
- Manage approval workflows.
- Execute authorized operational actions.
- Record execution outcomes.

This module is the only authorized application entry point for sensitive resolution actions.

### 5.2.5 Audit Management Module

Responsibilities:

- Record user actions.
- Record AI execution metadata.
- Record AI recommendations.
- Record approval decisions.
- Record resolution execution.
- Maintain audit correlation identifiers.

Audit records must be protected against unauthorized modification.

## 5.3 Level 2 — AI Platform Modules

### 5.3.1 AI Orchestrator

Technology: Python + LangGraph.

Responsibilities:

- Coordinate agent workflows.
- Manage workflow state.
- Execute conditional routing.
- Handle retries and timeouts.
- Manage human approval checkpoints.
- Maintain execution traces.

### 5.3.2 Machine Learning Service

Technology: Python + scikit-learn + XGBoost.

Responsibilities:

- Anomaly detection.
- Duplicate detection.
- Exception classification.
- Payment failure prediction.
- Transaction candidate ranking.

### 5.3.3 RAG Service

Responsibilities:

- Document ingestion.
- Text chunking.
- Embedding generation.
- Vector retrieval.
- Context preparation.
- Source attribution.

Knowledge sources:

- ISO 20022 documentation.
- Payment network specifications.
- Error code catalogues.
- Payment operational procedures.
- Internal operational knowledge.

### 5.3.4 Agent Services

| Agent | Main Responsibility |
|---|---|
| Reconciliation Agent | Investigate unmatched transactions |
| Payment Investigation Agent | Identify probable root causes |
| Payment Repair Agent | Recommend corrective actions |
| Operations Intelligence Agent | Identify operational patterns |
| Operations Copilot | Provide natural-language interaction |

## 5.4 Module Dependency Rules

1. Frontend must communicate with backend through APIs.
2. AI services must not directly access core banking database tables.
3. Core business services must not depend on LLM availability.
4. Agents must invoke authorized business tools through application interfaces.
5. Resolution execution must remain under the control of the Resolution Workflow Module.
6. Audit recording must cover all governed financial actions.
7. Infrastructure dependencies must be hidden behind appropriate application interfaces.

---

# 6. Runtime View

This section describes important runtime scenarios and component interactions.

## 6.1 Scenario 1 — Payment Transaction Ingestion

```mermaid
sequenceDiagram
    participant CBS as Banking Simulator
    participant API as Payment API
    participant PM as Payment Management
    participant DB as PostgreSQL
    participant K as Kafka

    CBS->>API: Submit payment transactions
    API->>PM: Validate transactions
    PM->>PM: Check idempotency
    PM->>DB: Persist valid transactions
    PM->>K: Publish PaymentReceived
    K-->>PM: Acknowledge event
    PM-->>API: Ingestion result
    API-->>CBS: Success response
```

Key architectural requirements:

- Idempotent ingestion.
- Data validation.
- Duplicate prevention.
- Transactional persistence.
- Reliable event publication.

The implementation should use the transactional outbox pattern to avoid inconsistencies between database commits and Kafka publication.

## 6.2 Scenario 2 — Payment Reconciliation

```mermaid
sequenceDiagram
    participant OPS as Operations User
    participant API as Application API
    participant RE as Reconciliation Engine
    participant DB as PostgreSQL
    participant EM as Exception Management

    OPS->>API: Start reconciliation batch
    API->>RE: Execute matching
    RE->>DB: Retrieve transactions
    RE->>RE: Apply deterministic matching rules
    RE->>DB: Persist matching results
    RE->>EM: Generate exceptions
    EM->>DB: Persist exception records
    API-->>OPS: Reconciliation summary
```

The reconciliation engine must be deterministic and repeatable.

## 6.3 Scenario 3 — AI Payment Investigation

```mermaid
sequenceDiagram
    participant EM as Exception Management
    participant AI as AI Orchestrator
    participant ML as ML Service
    participant RAG as RAG Service
    participant LLM as LLM Provider
    participant DB as PostgreSQL

    EM->>AI: Investigate exception
    AI->>DB: Retrieve transaction evidence
    AI->>ML: Classify exception
    ML-->>AI: Classification result
    AI->>RAG: Retrieve relevant documentation
    RAG-->>AI: Supporting knowledge
    AI->>LLM: Generate investigation analysis
    LLM-->>AI: Structured findings
    AI->>DB: Persist AI investigation
    AI-->>EM: Investigation report
```

The investigation report must contain:

- Exception classification.
- Probable root cause.
- Supporting evidence.
- Evidence references.
- Confidence and uncertainty information.
- Recommended next steps.

LLM-generated hypotheses must be clearly distinguished from verified transaction facts.

## 6.4 Scenario 4 — AI-Assisted Payment Resolution

```mermaid
sequenceDiagram
    participant OPS as Operations Officer
    participant AI as Payment Repair Agent
    participant WF as Resolution Workflow
    participant RULE as Business Rules
    participant API as Payment Action API
    participant AUD as Audit Management

    OPS->>AI: Request resolution recommendation
    AI->>RULE: Validate proposed action
    RULE-->>AI: Validation result
    AI-->>OPS: Resolution recommendation
    OPS->>WF: Approve action
    WF->>RULE: Revalidate authorization and policies
    RULE-->>WF: Approved
    WF->>API: Execute authorized action
    API-->>WF: Execution result
    WF->>AUD: Record decision and execution
    WF-->>OPS: Resolution status
```

Important controls:

- AI cannot approve its own recommendations.
- Approval must be bound to a specific action and its parameters.
- Authorization must be revalidated at execution time.
- Actions must be idempotent where applicable.
- All execution outcomes must be audited.

## 6.5 Scenario 5 — Operations Copilot

```mermaid
sequenceDiagram
    participant OPS as Operations Officer
    participant UI as Angular UI
    participant COP as Operations Copilot
    participant AUTH as Authorization Service
    participant DATA as Operational APIs
    participant RAG as RAG Service
    participant LLM as LLM Provider

    OPS->>UI: Ask operational question
    UI->>COP: Submit query
    COP->>AUTH: Validate access
    AUTH-->>COP: Authorized scope
    COP->>DATA: Retrieve operational data
    DATA-->>COP: Authorized results
    COP->>RAG: Retrieve supporting knowledge
    RAG-->>COP: Context
    COP->>LLM: Generate answer
    LLM-->>COP: Answer with evidence
    COP-->>UI: Return response
    UI-->>OPS: Display answer
```

The Copilot must not bypass the normal authorization model.

---

# 7. Deployment View

## 7.1 Deployment Strategy

The initial deployment runs locally without containers or external infrastructure.

The Spring Boot and Angular applications can be started with Maven and a local Node/npm workflow. H2, Spring Events, in-memory caching, mock authentication, simulated payment networks and mock AI responses provide a complete demonstration runtime.

PostgreSQL, Kafka, Redis, Keycloak and pgvector remain optional production adapters and are not required for a normal local demo.

## 7.2 Logical Deployment Architecture

```mermaid
flowchart TB
    USER[Browser / Operations User]

    subgraph LOCAL[Local Mock Runtime]
        WEB[Angular Frontend]
        BACKEND[Spring Boot Backend]
        AI[Mock AI Service or Optional FastAPI]
        EVENTS[Spring Events]
        CACHE[In-Memory Cache]
        DB[(H2 Database)]
        VECTOR[(In-Memory Vector Mock)]
        AUTH[Mock OIDC Provider]
        NETWORK[Payment Network Simulator]
    end

    USER --> WEB
    WEB --> BACKEND
    BACKEND --> AUTH
    BACKEND --> DB
    BACKEND --> CACHE
    BACKEND <--> EVENTS
    BACKEND <--> NETWORK
    EVENTS --> AI
    AI --> VECTOR
    AI --> BACKEND
```

## 7.3 Infrastructure Components

| Component | Deployment |
|---|---|
| Angular Frontend | Local development server |
| Spring Boot Backend | Maven runtime |
| Python AI Services | Mock implementation or optional local FastAPI process |
| Payment Network | In-process simulator |
| Authentication | Mock OIDC provider or local security configuration |
| Messaging | Spring Events |
| Database | H2 or PostgreSQL adapter |
| Cache | In-memory implementation or optional Redis adapter |
| Vector Search | In-memory implementation or optional pgvector adapter |
| Prometheus / Grafana | Optional local monitoring |

## 7.4 Environment Strategy

The platform should support:

- Local development.
- Integration testing.
- Staging / demonstration.
- Optional production-like deployment.

Sensitive credentials must be supplied through environment-specific secrets management rather than committed to source control.

## 7.5 Scalability Strategy

Initial architecture:

- Modular Spring Boot application.
- H2 or PostgreSQL persistence.
- In-process Spring Events with optional Kafka implementation.
- In-memory cache with optional Redis implementation.
- Optional independently scalable AI workers.

Future scaling options:

- Extract reconciliation into a dedicated service.
- Scale AI agents independently.
- Introduce dedicated ML model serving.
- Introduce separate event storage and analytical databases.
- Adopt managed Kafka and database infrastructure.

---

# 8. Cross-Cutting Concepts

## 8.1 Security Architecture

Security is implemented across application, infrastructure and AI layers.

### Authentication

Use mock OAuth 2.0 / OpenID Connect authentication locally.

The demo may use Spring Security with configured users and role claims. Keycloak remains an optional production identity provider.

### Authorization

Implement role-based access control.

Initial roles:

- PAYMENT_OPERATOR.
- RECONCILIATION_OFFICER.
- OPERATIONS_MANAGER.
- PAYMENT_ADMIN.
- COMPLIANCE_OFFICER.
- AI_GOVERNANCE_OFFICER.
- SYSTEM_ADMIN.

### Data Protection

- Encrypt data in transit.
- Encrypt data at rest.
- Mask sensitive information.
- Restrict AI provider access.
- Maintain secure secrets management.
- Apply data retention policies.

### AI Security

- Treat transaction records and retrieved documents as untrusted input.
- Protect against prompt injection.
- Restrict agent tools.
- Validate all tool parameters.
- Apply user authorization to data retrieval.
- Prevent unrestricted SQL execution.
- Maintain AI execution traces.

## 8.2 Data Architecture

The architecture separates operational data, knowledge data and AI execution data.

### Operational Data

The local profile stores operational records in file-backed H2 and applies schema changes through Flyway. PostgreSQL may be configured for external deployments.

Operational storage includes:

- `payment_record` stores internal and external transactions using a source discriminator.
- `payment_network` stores network configuration.
- `reconciliation_batch` and `reconciliation_result` store batch execution and matching evidence.
- `payment_exception` and `investigation` store discrepancy and investigation lifecycle state.
- `approval` stores human authorization decisions.
- `audit_log` stores actor, action, aggregate and event details.

Payment records are exposed through create, list and get operations under `/api/v1/payments`. API DTO validation and database constraints enforce required fields, unique source/reference pairs and decimal monetary precision.

### Knowledge Data

PostgreSQL + pgvector stores:

- Document metadata.
- Document chunks.
- Embeddings.
- Knowledge references.

### AI Execution Data

Stores:

- Agent execution IDs.
- Workflow states.
- Model versions.
- Recommendations.
- Confidence information.
- Evaluation metadata.

## 8.3 Event-Driven Architecture

Kafka is used for asynchronous communication.

Initial domain events:

| Event | Producer | Consumer |
|---|---|---|
| PaymentReceived | Payment Management | Reconciliation |
| ReconciliationStarted | Reconciliation | Monitoring |
| ReconciliationCompleted | Reconciliation | Exception Management |
| ExceptionDetected | Exception Management | AI Orchestrator |
| InvestigationRequested | Exception Management | AI Orchestrator |
| InvestigationCompleted | AI Orchestrator | Exception Management |
| ResolutionProposed | AI Orchestrator | Resolution Workflow |
| ResolutionApproved | Resolution Workflow | Action Processor |
| ResolutionExecuted | Action Processor | Audit / Monitoring |

Event schemas should be versioned and include correlation identifiers.

## 8.4 AI Architecture

### Model Strategy

Use a provider abstraction to avoid tight coupling to one LLM vendor.

Initial options:

- OpenAI models.
- Google Gemini models.

### Agent Orchestration

LangGraph manages:

- Agent state.
- Conditional routing.
- Tool execution.
- Human approval checkpoints.
- Workflow retries.
- Execution tracing.

### RAG Strategy

RAG is used to retrieve relevant information from trusted knowledge sources.

The system must provide evidence references and avoid presenting unsupported generated statements as facts.

### ML Strategy

Initial models:

- Isolation Forest for anomaly detection.
- XGBoost for exception classification.
- XGBoost for failure prediction.
- Candidate ranking for ambiguous transaction matching.

Models must be evaluated using labelled test datasets and appropriate baselines.

## 8.5 Observability

Use:

- Prometheus for metrics.
- Grafana for dashboards.
- OpenTelemetry for distributed tracing.
- Structured application logging.

Monitor:

- API latency.
- Reconciliation throughput.
- Kafka consumer lag.
- Exception volume.
- AI execution latency.
- LLM token consumption.
- AI agent failure rate.
- Recommendation acceptance rate.
- Model drift.
- Unauthorized action attempts.

## 8.6 Error Handling and Resilience

- Retry transient network failures.
- Apply circuit breakers to external dependencies.
- Use dead-letter queues.
- Implement idempotency.
- Apply timeouts to AI calls.
- Persist workflow state.
- Support resumable agent workflows.
- Provide manual fallback when AI is unavailable.

Core reconciliation must continue operating when the LLM provider is unavailable.

## 8.7 AI Evaluation and Governance

AI evaluation must be an architectural capability, not an afterthought.

Measure:

- Classification precision and recall.
- Anomaly detection quality.
- Root-cause analysis accuracy.
- Evidence grounding.
- Recommendation acceptance rate.
- Hallucination rate.
- Agent task completion rate.
- Latency and token consumption.
- Unauthorized action prevention.

Maintain versioned evaluation datasets and regression tests.

## 8.8 Testing Strategy

Testing layers:

1. Unit testing.
2. Integration testing.
3. API testing.
4. Contract testing.
5. Event-driven testing.
6. AI evaluation testing.
7. Security testing.
8. Performance testing.
9. End-to-end testing.

Financial integrity and authorization tests must be mandatory release gates.

---

# 9. Architecture Decisions

The following decisions establish the initial architectural direction.

| ADR | Decision | Rationale |
|---|---|---|
| ADR-001 | Modular Monolith for core application | Reduce initial complexity and preserve clear module boundaries |
| ADR-002 | Python for AI services | Mature AI and ML ecosystem |
| ADR-003 | Spring Boot for core banking services | Strong enterprise application and integration capabilities |
| ADR-004 | LangGraph for agent orchestration | Explicit stateful workflows and human approval checkpoints |
| ADR-005 | Hybrid AI architecture | Combine deterministic correctness with AI intelligence |
| ADR-006 | Kafka for asynchronous messaging | Decoupling and scalable event processing |
| ADR-007 | PostgreSQL as primary database | Transactional consistency and mature relational capabilities |
| ADR-008 | pgvector for initial vector search | Simplify infrastructure and knowledge retrieval |
| ADR-009 | Human approval for sensitive actions | Preserve financial accountability |
| ADR-010 | Provider abstraction for LLMs | Reduce vendor dependency |
| ADR-011 | Synthetic payment data | Enable safe and reproducible demonstrations |
| ADR-012 | ISO 20022-inspired data model | Demonstrate European payment domain knowledge |
| ADR-013 | Maven for Spring Boot API builds | Standardize Java dependency management, builds and test execution |

### ADR-001 — Modular Monolith vs Microservices

**Decision:** Adopt a modular monolith for core banking business capabilities.

**Context:** Introducing multiple independently deployed services from the beginning would increase operational complexity without sufficient demonstrated need.

**Consequences:**

Positive:
- Simpler development.
- Easier debugging.
- Lower deployment overhead.
- Strong transactional boundaries.

Negative:
- Requires strict module dependency management.
- Independent scaling of core modules is limited.

Future extraction should be based on measurable operational requirements.

### ADR-002 — Separation of AI and Core Banking Services

**Decision:** Deploy Python AI services separately from the Spring Boot core application.

**Context:** AI workloads have different resource requirements, deployment cycles and failure characteristics.

**Consequences:**

Positive:
- Independent scaling.
- Technology flexibility.
- Fault isolation.
- Easier AI model experimentation.

Negative:
- Additional network communication.
- More complex distributed tracing.
- Additional operational infrastructure.

### ADR-003 — Deterministic vs Probabilistic Processing

**Decision:** Financially significant transaction processing must remain deterministic.

**Context:** LLM outputs are probabilistic and cannot guarantee financial calculation accuracy or repeatable execution.

**Consequences:**

- Financial calculations use deterministic application code.
- Reconciliation rules are versioned.
- AI recommendations require validation.
- Human approvals govern sensitive actions.

### ADR-013 — Maven for Spring Boot API Development

**Decision:** Build and package the Java core application and REST APIs with Apache Maven.

**Context:** The Java backend requires consistent dependency resolution, repeatable builds, test execution and Spring Boot packaging.

**Consequences:**

- The repository uses a Maven `pom.xml` and Maven-compatible dependency declarations.
- Spring Boot applications are packaged with Maven plugins.
- CI/CD executes Maven build and test goals.
- Maven project conventions remain compatible with IDE and container build tooling.

---

# 10. Quality Requirements

## 10.1 Quality Scenarios

### QS-001 — High-Volume Reconciliation

**Scenario:** The system receives a batch containing 100,000 payment transactions.

**Expected response:**
- The system processes the batch without data loss.
- Matching results are persisted consistently.
- Exceptions are generated correctly.
- Processing throughput is measurable.

### QS-002 — LLM Provider Unavailability

**Scenario:** The external LLM provider becomes unavailable.

**Expected response:**
- Core reconciliation continues.
- Existing payment data remains accessible.
- AI investigation requests enter a retryable state.
- Users can perform manual investigation.

### QS-003 — Unauthorized Resolution Attempt

**Scenario:** An AI agent attempts to execute a financial action without valid approval.

**Expected response:**
- The backend rejects the action.
- No financial operation is performed.
- The attempt is recorded.
- Appropriate alerts are generated.

### QS-004 — High AI Latency

**Scenario:** An investigation requires longer processing time.

**Expected response:**
- The request is processed asynchronously.
- Workflow state is persisted.
- The user can monitor progress.
- Results are delivered when available.

### QS-005 — Duplicate Payment Event

**Scenario:** Kafka delivers the same payment event more than once.

**Expected response:**
- Idempotency prevents duplicate financial records.
- Event processing remains consistent.
- Duplicate event information is traceable.

## 10.2 Performance Targets

Initial demonstration targets:

| Metric | Target |
|---|---:|
| Reconciliation batch size | 100,000 transactions |
| Ordinary API response time | P95 below 500 ms |
| Reconciliation accuracy | At least 99% on defined test datasets |
| Duplicate detection precision | At least 99% on defined test datasets |
| AI investigation coverage | At least 85% of eligible exceptions |
| Investigation time reduction | At least 70% versus measured baseline |
| Governed action audit coverage | 100% |

These targets must be validated through performance and evaluation tests. They are not production service-level commitments.

---

# 11. Risks and Technical Debt

## 11.1 Architecture Risks

| Risk | Impact | Mitigation |
|---|---|---|
| LLM hallucination | Incorrect investigation conclusions | Evidence grounding and validation |
| AI prompt injection | Unauthorized information or actions | Input isolation and restricted tools |
| Incorrect ML predictions | Misclassified exceptions | Evaluation datasets and human review |
| External LLM dependency | AI service disruption | Provider abstraction and fallback |
| Kafka event duplication | Duplicate processing | Idempotent consumers |
| Data inconsistencies | Incorrect reconciliation | Transactional persistence and validation |
| Agent workflow failure | Incomplete investigations | Persisted state and retry mechanisms |
| Excessive LLM costs | Increased operating costs | Token budgets, caching and model selection |
| Data leakage | Exposure of sensitive banking data | Data masking, authorization and secure provider integration |
| Over-engineered architecture | Increased delivery complexity | Modular monolith and incremental delivery |

## 11.2 Initial Technical Debt

The following items may be deferred during the showcase implementation:

- Production payment network connectivity.
- Advanced distributed model serving.
- Dedicated vector database infrastructure.
- Multi-region deployment.
- Advanced active-active disaster recovery.
- Full enterprise secrets management integration.
- Production-grade regulatory compliance certification.

These limitations must be documented and must not be misrepresented as production-ready capabilities.

---

# 12. Glossary

| Term | Definition |
|---|---|
| ARUS AI | Intelligent Payment Operations Platform |
| Payment Reconciliation | Process of comparing internal and external payment records |
| Payment Exception | A transaction discrepancy requiring investigation |
| Settlement | Final transfer or discharge of payment obligations |
| ISO 20022 | International financial messaging standard |
| SEPA | Single Euro Payments Area |
| AI Agent | AI component capable of reasoning and invoking authorized tools |
| Agentic AI | AI system that executes multi-step tasks using controlled workflows |
| RAG | Retrieval-Augmented Generation |
| LLM | Large Language Model |
| ML | Machine Learning |
| Human-in-the-Loop | Human involvement in decisions and approvals |
| Deterministic Processing | Processing that produces predictable results for identical inputs and conditions |
| Probabilistic Processing | Processing involving statistical uncertainty |
| Idempotency | Property ensuring repeated requests do not create unintended duplicate effects |
| Outbox Pattern | Pattern for reliably publishing events associated with database transactions |
| RBAC | Role-Based Access Control |
| ADR | Architecture Decision Record |
| P95 | 95th percentile response time |

---

# Appendix A — Recommended Repository Structure

```text
arus-ai/
│
├── README.md
├── REQUIREMENTS.md
├── arc42.md
├── LICENSE
├── pom.xml
├── mvnw
├── mvnw.cmd
│
├── docs/
│   ├── architecture/
│   │   ├── diagrams/
│   │   │   ├── system-context.mmd
│   │   │   ├── container.mmd
│   │   │   ├── component.mmd
│   │   │   ├── deployment.mmd
│   │   │   └── sequence/
│   │   ├── adr/
│   │   │   ├── ADR-001-modular-monolith.md
│   │   │   ├── ADR-002-ai-service-separation.md
│   │   │   └── ADR-003-hybrid-ai.md
│   │   └── architecture-decisions.md
│   │
│   ├── api/
│   ├── domain/
│   ├── ai/
│   ├── security/
│   └── deployment/
│
├── frontend/
│   └── arus-web/
│
├── backend/
│   └── arus-core/
│       └── pom.xml
│
├── ai/
│   └── arus-intelligence/
│
├── infrastructure/
│   ├── kafka/
│   ├── database/
│   └── monitoring/
│
├── datasets/
│   ├── payments/
│   ├── reconciliation/
│   └── exceptions/
│
├── scripts/
│
└── tests/
    ├── integration/
    ├── performance/
    ├── security/
    └── ai-evaluation/
```

---

# Appendix B — Architecture Documentation Governance

This document should evolve alongside the implementation.

Update the architecture documentation whenever:

- A major architectural decision changes.
- A new integration is introduced.
- A new AI agent is added.
- A business module is extracted.
- A security or governance mechanism changes.
- A significant quality attribute is modified.

Architecture decisions must be recorded in ADRs rather than silently changing the architecture.

**Document owner:** ARUS AI Architecture Team
**Review frequency:** At major development milestones
**Next review:** Before Phase 1 implementation

---

**End of Document**

*ARUS AI — Intelligence Behind Every Transaction.*