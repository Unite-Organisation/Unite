-- A guest the host threw out keeps their row (status BANNED) so the same code or email cannot come
-- back, but their name goes free: someone else may join under it. Only guests still in the event
-- hold a name.
DROP INDEX idx_event_member_guest_name;

CREATE UNIQUE INDEX idx_event_member_guest_name ON event_member (event_id, lower(display_name))
    WHERE user_id IS NULL AND status <> 'BANNED';
