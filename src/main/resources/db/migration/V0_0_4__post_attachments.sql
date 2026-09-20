ALTER TABLE post DROP COLUMN image_reference;

ALTER TABLE post ADD COLUMN attachments JSONB NOT NULL DEFAULT '[]'::jsonb;
