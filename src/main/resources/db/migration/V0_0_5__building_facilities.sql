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