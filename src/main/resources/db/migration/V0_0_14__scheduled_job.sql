CREATE TABLE scheduled_job
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name     VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    run_at       TIMESTAMP    NOT NULL,
    status       VARCHAR(20)  NOT NULL,

    attempts     INT          NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    last_error   TEXT,
    dedupe_key   VARCHAR(128),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT scheduled_job_attempts_not_negative CHECK (attempts >= 0)
);

CREATE INDEX idx_scheduled_job_due ON scheduled_job (run_at) WHERE status = 'PENDING';
CREATE INDEX idx_scheduled_job_expired_lease ON scheduled_job (locked_until) WHERE status = 'RUNNING';
CREATE UNIQUE INDEX idx_scheduled_job_dedupe ON scheduled_job (dedupe_key)
    WHERE dedupe_key IS NOT NULL AND status IN ('PENDING', 'RUNNING');
