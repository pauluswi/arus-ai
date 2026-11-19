CREATE TABLE payment_record (
    id UUID PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL,
    end_to_end_id VARCHAR(100),
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    counterparty VARCHAR(140) NOT NULL,
    value_date DATE NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_payment_record_source_reference UNIQUE (source, transaction_reference)
);

CREATE INDEX idx_payment_record_end_to_end_id ON payment_record (end_to_end_id);
CREATE INDEX idx_payment_record_value_date ON payment_record (value_date);
CREATE INDEX idx_payment_record_status ON payment_record (status);
