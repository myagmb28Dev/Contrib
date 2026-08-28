ALTER TABLE activity_event
    DROP CONSTRAINT fk_activity_snapshot,
    ADD CONSTRAINT fk_activity_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES repository_snapshot (id) ON DELETE CASCADE;

ALTER TABLE contribution_analysis
    DROP CONSTRAINT fk_analysis_snapshot,
    ADD CONSTRAINT fk_analysis_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES repository_snapshot (id) ON DELETE CASCADE;

ALTER TABLE repository_snapshot
    DROP CONSTRAINT fk_snapshot_job,
    ADD CONSTRAINT fk_snapshot_job
        FOREIGN KEY (analysis_job_id) REFERENCES analysis_job (id) ON DELETE CASCADE;

ALTER TABLE repository_snapshot
    DROP CONSTRAINT fk_snapshot_repository,
    ADD CONSTRAINT fk_snapshot_repository
        FOREIGN KEY (repository_id) REFERENCES github_repository (id) ON DELETE CASCADE;

ALTER TABLE analysis_job
    DROP CONSTRAINT fk_analysis_job_repository,
    ADD CONSTRAINT fk_analysis_job_repository
        FOREIGN KEY (repository_id) REFERENCES github_repository (id) ON DELETE CASCADE;

ALTER TABLE certificate
    DROP CONSTRAINT fk_certificate_analysis,
    ADD CONSTRAINT fk_certificate_analysis
        FOREIGN KEY (analysis_id) REFERENCES contribution_analysis (id) ON DELETE CASCADE;

ALTER TABLE blockchain_attestation
    DROP CONSTRAINT fk_attestation_certificate,
    ADD CONSTRAINT fk_attestation_certificate
        FOREIGN KEY (certificate_id) REFERENCES certificate (id) ON DELETE CASCADE;
