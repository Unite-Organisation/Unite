-- The public link cannot be taken back without sending a new one to everybody, so a host who had to
-- throw someone out makes every newcomer wait for approval instead. One way only: once an event
-- screens its members, it keeps doing so - there is no state where half of them were let in freely.
--
-- A member waiting for it has status PENDING_APPROVAL (event_member.status is a plain VARCHAR), and
-- the queue is the order they asked in: event_member.created_at.
ALTER TABLE event ADD COLUMN requires_approval BOOLEAN NOT NULL DEFAULT FALSE;
