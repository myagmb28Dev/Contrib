CREATE TABLE github_repository (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    github_repository_id BIGINT NOT NULL,
    owner_github_id BIGINT NOT NULL,
    owner_login VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    full_name VARCHAR(512) NOT NULL,
    html_url VARCHAR(1024) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    default_branch VARCHAR(255) NOT NULL,
    language VARCHAR(255),
    archived BOOLEAN NOT NULL,
    last_synced_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_github_repository_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uk_github_repository_user_repo UNIQUE (user_id, github_repository_id)
);

CREATE INDEX idx_github_repository_user ON github_repository (user_id);

CREATE TABLE analysis_job (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    repository_id UUID NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    collector_version VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    progress INTEGER NOT NULL,
    error_code VARCHAR(128),
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_analysis_job_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_analysis_job_repository FOREIGN KEY (repository_id) REFERENCES github_repository (id),
    CONSTRAINT uk_analysis_job_idempotency UNIQUE (
        user_id, repository_id, period_start, period_end, collector_version
    )
);

CREATE TABLE repository_snapshot (
    id UUID PRIMARY KEY,
    analysis_job_id UUID NOT NULL UNIQUE,
    repository_id UUID NOT NULL,
    subject_github_id BIGINT NOT NULL,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    collector_version VARCHAR(64) NOT NULL,
    collected_at TIMESTAMPTZ NOT NULL,
    source_metadata TEXT NOT NULL,
    snapshot_hash VARCHAR(66) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_snapshot_job FOREIGN KEY (analysis_job_id) REFERENCES analysis_job (id),
    CONSTRAINT fk_snapshot_repository FOREIGN KEY (repository_id) REFERENCES github_repository (id)
);

CREATE TABLE activity_event (
    id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    author_github_id BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    title TEXT,
    state VARCHAR(64),
    additions INTEGER NOT NULL,
    deletions INTEGER NOT NULL,
    changed_files INTEGER NOT NULL,
    raw_payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_activity_snapshot FOREIGN KEY (snapshot_id) REFERENCES repository_snapshot (id),
    CONSTRAINT uk_activity_snapshot_external UNIQUE (snapshot_id, type, external_id)
);

CREATE INDEX idx_activity_snapshot_occurred ON activity_event (snapshot_id, occurred_at);

CREATE TABLE contribution_analysis (
    id UUID PRIMARY KEY,
    snapshot_id UUID NOT NULL UNIQUE,
    metrics TEXT NOT NULL,
    score INTEGER NOT NULL,
    score_version VARCHAR(64) NOT NULL,
    calculation_rules TEXT NOT NULL,
    technical_areas TEXT NOT NULL,
    ai_summary TEXT,
    ai_model VARCHAR(255),
    ai_prompt_version VARCHAR(64),
    ai_regeneration_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_analysis_snapshot FOREIGN KEY (snapshot_id) REFERENCES repository_snapshot (id)
);
