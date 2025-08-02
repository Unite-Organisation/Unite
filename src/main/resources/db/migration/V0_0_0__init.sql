CREATE TABLE users
(

    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(20) NOT NULL,
    last_name  VARCHAR(20) NOT NULL

);