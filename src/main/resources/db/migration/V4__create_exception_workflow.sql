CREATE TABLE investigation_note (
    id UUID PRIMARY KEY,
    exception_id UUID NOT NULL REFERENCES payment_exception (id),
    author VARCHAR(120) NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE exception_resolution_request (
    id UUID PRIMARY KEY,
    exception_id UUID NOT NULL REFERENCES payment_exception (id),
    action VARCHAR(16) NOT NULL,
    requested_by VARCHAR(120) NOT NULL,
    justification VARCHAR(1000) NOT NULL,
    return_status VARCHAR(24) NOT NULL,
    status VARCHAR(16) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_investigation_note_exception
    ON investigation_note (exception_id, created_at);
CREATE INDEX idx_exception_resolution_request_pending
    ON exception_resolution_request (exception_id, status, requested_at);
