-- An event is its own entity, so it can exist outside any building: anyone holding its link can join.
-- A building event is an event plus a post that shows it in the building feed.

CREATE TABLE event
(
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_slug      VARCHAR(16)  NOT NULL UNIQUE,
    building_id      UUID REFERENCES building (id) ON DELETE CASCADE,
    name             VARCHAR(256) NOT NULL,
    description      TEXT,
    start_date_time  TIMESTAMP,
    end_date_time    TIMESTAMP,
    location_name    VARCHAR(256),
    online_url       VARCHAR(2048),
    max_attendees    INT,
    waitlist_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT event_max_attendees_positive
        CHECK (max_attendees IS NULL OR max_attendees > 0),
    CONSTRAINT event_dates_ordered
        CHECK (start_date_time IS NULL OR end_date_time IS NULL OR start_date_time <= end_date_time),
    CONSTRAINT event_waitlist_needs_limit
        CHECK (NOT waitlist_enabled OR max_attendees IS NOT NULL)
);

CREATE INDEX idx_event_building ON event (building_id) WHERE building_id IS NOT NULL;

-- One row per person per event, whether they came from Unite (user_id) or from the link (pin_hash).
-- Everything attached to a person inside an event (sessions, later messages) references this row only.
CREATE TABLE event_member
(
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id          UUID        NOT NULL REFERENCES event (id) ON DELETE CASCADE,
    user_id           UUID REFERENCES app_user (id) ON DELETE CASCADE,
    display_name      VARCHAR(60) NOT NULL,
    role              VARCHAR(10) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    status_changed_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pin_hash          VARCHAR(60),
    failed_attempts   INT         NOT NULL DEFAULT 0,
    locked_until      TIMESTAMP,
    email           VARCHAR(254),
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT event_member_single_identity CHECK ((user_id IS NULL) <> (pin_hash IS NULL))
);

CREATE UNIQUE INDEX idx_event_member_user ON event_member (event_id, user_id) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX idx_event_member_guest_name ON event_member (event_id, lower(display_name)) WHERE user_id IS NULL;
CREATE UNIQUE INDEX idx_event_member_host ON event_member (event_id) WHERE role = 'HOST';
CREATE INDEX idx_event_member_status ON event_member (event_id, status, status_changed_at);
CREATE INDEX idx_event_member_app_user ON event_member (user_id) WHERE user_id IS NOT NULL;

-- Sessions of every member, guest or Unite user alike - the only credential event endpoints accept.
-- How the member proved who they are (PIN or Unite JWT) matters only when a session is opened.
CREATE TABLE event_member_session
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id  UUID        NOT NULL REFERENCES event_member (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_event_member_session_member ON event_member_session (member_id);

-- The event owns its data, an event post keeps only what the feed needs.
ALTER TABLE post ADD COLUMN event_id UUID UNIQUE REFERENCES event (id) ON DELETE CASCADE;

ALTER TABLE post ALTER COLUMN name DROP NOT NULL;
ALTER TABLE post ALTER COLUMN content DROP NOT NULL;

ALTER TABLE post DROP COLUMN start_date_time;
ALTER TABLE post DROP COLUMN end_date_time;
ALTER TABLE post DROP COLUMN location_name;
ALTER TABLE post DROP COLUMN online_url;
ALTER TABLE post DROP COLUMN max_attendees;