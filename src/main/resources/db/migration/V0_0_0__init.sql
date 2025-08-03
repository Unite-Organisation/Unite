CREATE TABLE user_roles
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_role VARCHAR(15) NOT NULL
);

INSERT INTO user_roles(user_role)
VALUES
    ('STANDARD'),
    ('MANAGER'),
    ('ADMIN');

CREATE TABLE users
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(20) NOT NULL,
    last_name  VARCHAR(20) NOT NULL,
    email VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(50) NOT NULL,
    user_role UUID REFERENCES user_roles(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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