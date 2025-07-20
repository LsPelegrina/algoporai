CREATE TABLE payment_audit (
                               correlation_id UUID PRIMARY KEY,
                               amount NUMERIC(18,2) NOT NULL,
                               processor VARCHAR(20) NOT NULL, -- "default" ou "fallback"
                               requested_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_payment_audit_processor_requested_at
    ON payment_audit(processor, requested_at);
