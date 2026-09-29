-- Keep GitHub's creation timestamp separate from the local row's created_at.
ALTER TABLE github_repository ADD COLUMN github_created_at TIMESTAMPTZ;
