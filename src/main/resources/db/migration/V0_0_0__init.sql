CREATE TABLE user_roles
(
    id UUID PRIMARY KEY,
    user_role VARCHAR(15) NOT NULL
);

INSERT INTO user_roles (id, user_role) VALUES
    ('a3f5c9d2-4b8e-4d61-9a67-12c4e9b7f8a1', 'STANDARD'),
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
    area_id UUID REFERENCES areas(id) ON DELETE CASCADE
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
    building_id UUID REFERENCES buildings(id) ON DELETE CASCADE,
    area_id UUID REFERENCES areas(id) ON DELETE CASCADE
);

CREATE TABLE areas_users
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    area_id UUID REFERENCES areas(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
                                         reservation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE CASCADE,
                                         user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                         start_time TIMESTAMP NOT NULL,
                                         end_time TIMESTAMP NOT NULL,
                                         status VARCHAR(50) NOT NULL,
                                         purpose VARCHAR(255),
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         UNIQUE (facility_id, start_time, end_time)
);



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