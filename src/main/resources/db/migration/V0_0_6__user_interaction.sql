CREATE TABLE user_interaction
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    entity_id UUID NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    interaction_type VARCHAR(50) NOT NULL,

    CONSTRAINT user_interaction_unique UNIQUE (entity_type, entity_id, interaction_type, user_id)
);

-- supports "my interactions" lookups and the ON DELETE CASCADE from app_user
CREATE INDEX idx_user_interaction_user ON user_interaction (user_id);
