CREATE TABLE payment_import_request (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    request_hash VARCHAR(64) NOT NULL,
    response_payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
