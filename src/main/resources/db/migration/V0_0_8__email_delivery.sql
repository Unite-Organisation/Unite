CREATE TABLE email_delivery
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_type VARCHAR(50) NOT NULL,
    reference_id  UUID        NOT NULL,
    user_id       UUID        REFERENCES app_user(id) ON DELETE CASCADE,
    email         VARCHAR(50) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at       TIMESTAMP,
    CONSTRAINT uq_email_delivery UNIQUE (delivery_type, reference_id, user_id)
);

CREATE INDEX idx_email_delivery_unfinished ON email_delivery (status) WHERE status <> 'SENT';
