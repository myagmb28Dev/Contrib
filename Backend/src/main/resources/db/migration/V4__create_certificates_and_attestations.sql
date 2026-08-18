CREATE TABLE certificate (
    id UUID PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    analysis_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    schema_version VARCHAR(32) NOT NULL,
    canonical_payload TEXT NOT NULL,
    certificate_hash VARCHAR(66) NOT NULL UNIQUE,
    issuer_wallet_address VARCHAR(42),
    subject_wallet_address VARCHAR(42),
    status VARCHAR(32) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revocation_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_certificate_analysis FOREIGN KEY (analysis_id) REFERENCES contribution_analysis (id),
    CONSTRAINT fk_certificate_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE TABLE blockchain_attestation (
    id UUID PRIMARY KEY,
    certificate_id UUID NOT NULL UNIQUE,
    chain_id BIGINT NOT NULL,
    network VARCHAR(64) NOT NULL,
    contract_address VARCHAR(42) NOT NULL,
    onchain_certificate_id VARCHAR(66) NOT NULL,
    transaction_hash VARCHAR(66) NOT NULL UNIQUE,
    block_number BIGINT,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_attestation_certificate FOREIGN KEY (certificate_id) REFERENCES certificate (id)
);
