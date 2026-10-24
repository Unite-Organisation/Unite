-- event_member.email (V0_0_12) already holds what a guest typed in; the delivery log has to be able to
-- keep any address a member can have, not just the 50 characters an app_user is limited to
ALTER TABLE email_delivery
    ALTER COLUMN email TYPE VARCHAR(254);
