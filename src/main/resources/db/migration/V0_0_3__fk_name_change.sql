ALTER TABLE offering
    DROP CONSTRAINT IF EXISTS offerings_area_id_fkey;

ALTER TABLE offering
    ADD CONSTRAINT offerings_area_id_fkey
        FOREIGN KEY (area_id)
            REFERENCES areas(id)
            ON DELETE CASCADE;