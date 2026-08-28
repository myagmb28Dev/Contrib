ALTER TABLE analysis_job ADD COLUMN target_branch VARCHAR(255);

UPDATE analysis_job aj
SET target_branch = gr.default_branch
FROM github_repository gr
WHERE aj.repository_id = gr.id AND aj.target_branch IS NULL;

UPDATE analysis_job
SET target_branch = 'main'
WHERE target_branch IS NULL;

ALTER TABLE analysis_job ALTER COLUMN target_branch SET NOT NULL;

ALTER TABLE analysis_job DROP CONSTRAINT IF EXISTS uk_analysis_job_idempotency;

ALTER TABLE analysis_job ADD CONSTRAINT uk_analysis_job_idempotency UNIQUE (
    user_id, repository_id, period_start, period_end, collector_version, target_branch
);
