-- What the device of a member looked like when they joined, so the member list can be ordered by how
-- likely each entry is the caller. It only orders the list - the return code still decides who gets in.
-- One row per member per device: a member who comes back from a new phone gets a second row, and the
-- best matching row is what counts.
CREATE TABLE event_member_metadata
(
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id            UUID        NOT NULL REFERENCES event_member (id) ON DELETE CASCADE,
    -- identifies the device within a member, so a returning device updates its row instead of adding one
    device_key           VARCHAR(64) NOT NULL,

    -- hashed: equality is all the scoring needs, so a leaked database yields no usable fingerprints
    device_id_hash       VARCHAR(64),
    ip_hash              VARCHAR(64),
    ip_network_hash      VARCHAR(64),
    user_agent_hash      VARCHAR(64),
    renderer_hash        VARCHAR(64),
    canvas_hash          VARCHAR(64),

    -- low entropy on their own, kept readable so the weights can be tuned against real data
    os_family            VARCHAR(20),
    os_version           VARCHAR(20),
    browser_family       VARCHAR(20),
    browser_major        INT,
    device_model         VARCHAR(60),
    screen               VARCHAR(20),
    pixel_ratio          NUMERIC(4, 2),
    color_depth          INT,
    hardware_concurrency INT,
    device_memory        NUMERIC(4, 1),
    max_touch_points     INT,
    time_zone            VARCHAR(40),
    time_zone_offset     INT,
    languages            VARCHAR(120),

    created_at           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_event_member_metadata_device ON event_member_metadata (member_id, device_key);
