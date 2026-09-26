-- An event is created one of two ways. FIXED is what existed until now: the host already has a
-- date and looks for people. POLL is the new one: the host has people in mind and no date, so the
-- date is what the group decides - every member marks which of the proposed slots work for them,
-- and the event starts once one slot carries enough of them.
--
-- Existing rows default to FIXED/CONFIRMED, which is exactly what they already were.

ALTER TABLE event ADD COLUMN scheduling_mode  VARCHAR(20) NOT NULL DEFAULT 'FIXED';
ALTER TABLE event ADD COLUMN status           VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';
-- how many members make the event happen; only POLL has one
ALTER TABLE event ADD COLUMN min_attendees    INT;
-- after this the slots stop being a choice: the system keeps the best one and the rest are dropped
ALTER TABLE event ADD COLUMN voting_deadline  TIMESTAMP;
ALTER TABLE event ADD COLUMN selected_slot_id UUID;
-- end of the window in which a member may still walk out after the group formed
ALTER TABLE event ADD COLUMN confirm_by       TIMESTAMP;

-- A date the host proposed. Only POLL events have these, at least two of them, and exactly one of
-- them survives - it is copied onto the event itself once the event is confirmed, so everything
-- already reading event.start_date_time keeps working without knowing any of this happened.
CREATE TABLE event_slot
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID      NOT NULL REFERENCES event (id) ON DELETE CASCADE,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time   TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT event_slot_dates_ordered
        CHECK (end_date_time IS NULL OR start_date_time <= end_date_time)
);

CREATE UNIQUE INDEX idx_event_slot_start ON event_slot (event_id, start_date_time);

ALTER TABLE event ADD CONSTRAINT event_selected_slot_fk
    FOREIGN KEY (selected_slot_id) REFERENCES event_slot (id) ON DELETE SET NULL;

-- How one member feels about one slot. No row means the slot does not work for them at all;
-- PREFERRED means they can make it; IF_NEEDED means they would rather not, but will if this slot
-- is what makes the group happen. The difference is the whole point - a slot carried by people
-- who actually want it beats a slot carried by people talked into it.
CREATE TABLE event_slot_vote
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id    UUID        NOT NULL REFERENCES event_slot (id) ON DELETE CASCADE,
    member_id  UUID        NOT NULL REFERENCES event_member (id) ON DELETE CASCADE,
    preference VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_event_slot_vote_member ON event_slot_vote (slot_id, member_id);
CREATE INDEX idx_event_slot_vote_by_member ON event_slot_vote (member_id);

-- a threshold and a deadline are what make a POLL event resolvable; a FIXED one has neither
ALTER TABLE event ADD CONSTRAINT event_poll_mode_fields CHECK (
       (scheduling_mode = 'FIXED' AND min_attendees IS NULL AND voting_deadline IS NULL)
    OR (scheduling_mode = 'POLL' AND min_attendees IS NOT NULL AND voting_deadline IS NOT NULL)
);

ALTER TABLE event ADD CONSTRAINT event_min_attendees_positive
    CHECK (min_attendees IS NULL OR min_attendees > 0);

-- a group that can never reach its own threshold would collect votes until the deadline for nothing
ALTER TABLE event ADD CONSTRAINT event_min_not_above_max
    CHECK (min_attendees IS NULL OR max_attendees IS NULL OR min_attendees <= max_attendees);

-- the walk-out window always refers to a slot that was already chosen
ALTER TABLE event ADD CONSTRAINT event_confirm_by_needs_slot
    CHECK (confirm_by IS NULL OR selected_slot_id IS NOT NULL);
