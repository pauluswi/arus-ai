CREATE TABLE payment_network (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE reconciliation_batch (
    id UUID PRIMARY KEY,
    status VARCHAR(24) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    initiated_by VARCHAR(120) NOT NULL
);

CREATE TABLE reconciliation_result (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES reconciliation_batch (id),
    internal_record_id UUID REFERENCES payment_record (id),
    external_record_id UUID REFERENCES payment_record (id),
    status VARCHAR(24) NOT NULL,
    matching_evidence TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_reconciliation_result_has_record
        CHECK (internal_record_id IS NOT NULL OR external_record_id IS NOT NULL)
);

CREATE TABLE payment_exception (
    id UUID PRIMARY KEY,
    reconciliation_result_id UUID NOT NULL REFERENCES reconciliation_result (id),
    category VARCHAR(40) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    priority INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL,
    assigned_to VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_payment_exception_priority CHECK (priority BETWEEN 1 AND 5)
);

CREATE TABLE investigation (
    id UUID PRIMARY KEY,
    exception_id UUID NOT NULL UNIQUE REFERENCES payment_exception (id),
    status VARCHAR(24) NOT NULL,
    summary TEXT,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE approval (
    id UUID PRIMARY KEY,
    target_type VARCHAR(40) NOT NULL,
    target_id UUID NOT NULL,
    decision VARCHAR(24) NOT NULL,
    decided_by VARCHAR(120) NOT NULL,
    justification VARCHAR(1000) NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    actor VARCHAR(120) NOT NULL,
    action VARCHAR(80) NOT NULL,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id UUID NOT NULL,
    details TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_reconciliation_result_batch ON reconciliation_result (batch_id);
CREATE INDEX idx_payment_exception_status ON payment_exception (status);
CREATE INDEX idx_investigation_status ON investigation (status);
CREATE INDEX idx_approval_target ON approval (target_type, target_id);
CREATE INDEX idx_audit_log_aggregate ON audit_log (aggregate_type, aggregate_id, occurred_at);
