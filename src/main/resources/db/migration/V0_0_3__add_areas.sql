CREATE TABLE areas
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(30) NOT NULL,
    country  VARCHAR(30) NOT NULL,
    city VARCHAR(30) NOT NULL,
    type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE areas_users
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_id UUID REFERENCES areas(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);