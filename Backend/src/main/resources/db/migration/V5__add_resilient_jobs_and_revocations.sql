ALTER TABLE analysis_job
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD COLUMN lease_expires_at TIMESTAMPTZ;

UPDATE analysis_job
SET next_attempt_at = created_at
WHERE status = 'PENDING';

CREATE INDEX idx_analysis_job_dispatch
    ON analysis_job (status, next_attempt_at, lease_expires_at, created_at);

ALTER TABLE blockchain_attestation
    ADD COLUMN revocation_transaction_hash VARCHAR(66),
    ADD COLUMN revocation_block_number BIGINT,
    ADD COLUMN revocation_status VARCHAR(32),
    ADD COLUMN revocation_reason TEXT,
    ADD COLUMN revocation_submitted_at TIMESTAMPTZ,
    ADD COLUMN revocation_confirmed_at TIMESTAMPTZ;

ALTER TABLE blockchain_attestation
    ADD CONSTRAINT uk_attestation_revocation_transaction UNIQUE (revocation_transaction_hash);
