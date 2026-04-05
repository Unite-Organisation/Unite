CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at  TIMESTAMP NULL,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    is_used     BOOLEAN DEFAULT FALSE
);