ALTER TABLE post ADD COLUMN visible_from TIMESTAMP;
ALTER TABLE post ADD COLUMN visible_to   TIMESTAMP;

ALTER TABLE post
    ADD CONSTRAINT visible_window_valid
        CHECK (visible_from IS NULL OR visible_to IS NULL OR visible_from <= visible_to);
