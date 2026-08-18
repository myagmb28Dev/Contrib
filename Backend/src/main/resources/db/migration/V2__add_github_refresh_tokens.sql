ALTER TABLE github_account
    ALTER COLUMN encrypted_access_token DROP NOT NULL,
    ADD COLUMN access_token_issued_at TIMESTAMPTZ,
    ADD COLUMN encrypted_refresh_token TEXT,
    ADD COLUMN refresh_token_issued_at TIMESTAMPTZ,
    ADD COLUMN refresh_token_expires_at TIMESTAMPTZ;
