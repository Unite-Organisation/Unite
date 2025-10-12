CREATE TABLE user_roles
(
    id UUID PRIMARY KEY,
    user_role VARCHAR(15) NOT NULL
);

INSERT INTO user_roles (id, user_role) VALUES
    ('a3f5c9d2-4b8e-4d61-9a67-12c4e9b7f8a1', 'RESIDENT'),
    ('b9e2a7f4-6c1d-47d2-8e93-45ab2c1d3f27', 'MANAGER'),
    ('c4d8e1a9-9f2b-4c6f-82d5-67d8a1c2e5b3', 'ADMIN');


CREATE TABLE areas
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(30) NOT NULL,
    country  VARCHAR(30) NOT NULL,
    city VARCHAR(30) NOT NULL,
    type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE buildings
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(30),
    country  VARCHAR(30),
    city VARCHAR(30),
    street VARCHAR(50),
    number VARCHAR(10),
    area_id UUID REFERENCES areas(id) ON DELETE CASCADE NOT NULL
);

CREATE TABLE users
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(20) NOT NULL,
    last_name  VARCHAR(20) NOT NULL,
    email      VARCHAR(50) UNIQUE,
    username   VARCHAR(20) NOT NULL UNIQUE,
    password   VARCHAR(200) NOT NULL,
    user_role  UUID REFERENCES user_roles(id) ON DELETE CASCADE,
    status     VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    building_id UUID REFERENCES buildings(id) ON DELETE CASCADE
);

CREATE TABLE buildings_managers
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID REFERENCES buildings(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL
);

CREATE TABLE facilities
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    building_id UUID REFERENCES buildings(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    capacity INT,
    location VARCHAR(255),
    requires_approval BOOLEAN DEFAULT FALSE
);

CREATE TABLE facilities_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    purpose VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (facility_id, start_time, end_time)
);

CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(256) NOT NULL,
    area_id UUID REFERENCES areas(id) ON DELETE CASCADE,
    building_id UUID REFERENCES buildings(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    content TEXT NOT NULL,
    image_reference VARCHAR(2048),
    related_date TIMESTAMP
);

ALTER TABLE announcements
    ADD CONSTRAINT area_or_building_not_both_null_or_not_null_ann
        CHECK (
            (area_id IS NULL AND building_id IS NOT NULL)
                OR
            (area_id IS NOT NULL AND building_id IS NULL)
            );


CREATE TABLE polls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    area_id UUID REFERENCES areas(id) ON DELETE CASCADE,
    building_id UUID REFERENCES buildings(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    anonymous BOOLEAN NOT NULL,
    finished BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE polls
    ADD CONSTRAINT area_or_building_not_both_null_or_not_null_polls
        CHECK (
            (area_id IS NULL AND building_id IS NOT NULL)
                OR
            (area_id IS NOT NULL AND building_id IS NULL)
            );

CREATE TABLE poll_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_text VARCHAR(255) NOT NULL,
    option_votes INT NOT NULL DEFAULT 0,
    UNIQUE(poll_id, option_text)
);

CREATE TABLE poll_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(poll_id, user_id)
);

CREATE TABLE poll_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    voters_count INT,
    voting_ended BOOLEAN NOT NULL
);

CREATE TABLE poll_winner (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_id UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE
);


 --messaging section
CREATE TABLE conversations
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    is_group BOOLEAN NOT NULL,
    name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE conversation_member
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    join_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE message
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    content TEXT,
    send_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE messages_read
(
    message_id UUID REFERENCES message(id) ON DELETE CASCADE,
    viewed_by UUID REFERENCES users(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(message_id, viewed_by)
);


-- triggers
CREATE OR REPLACE FUNCTION enforce_user_building_id_rule()
    RETURNS TRIGGER AS $$
DECLARE
    role_name TEXT;
BEGIN
    SELECT user_role INTO role_name
    FROM user_roles
    WHERE id = NEW.user_role;

    IF role_name IN ('ADMIN', 'MANAGER') AND NEW.building_id IS NOT NULL THEN
        RAISE EXCEPTION 'Users with role % cannot have building_id set', role_name;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_user_building_id
    BEFORE INSERT OR UPDATE ON users
    FOR EACH ROW
EXECUTE FUNCTION enforce_user_building_id_rule();
