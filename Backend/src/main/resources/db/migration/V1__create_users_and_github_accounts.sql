CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE github_account (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    github_user_id BIGINT NOT NULL UNIQUE,
    github_username VARCHAR(255) NOT NULL,
    email VARCHAR(320),
    encrypted_access_token TEXT NOT NULL,
    token_expires_at TIMESTAMPTZ,
    connected_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_github_account_user
        FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_github_account_username
    ON github_account (github_username);
