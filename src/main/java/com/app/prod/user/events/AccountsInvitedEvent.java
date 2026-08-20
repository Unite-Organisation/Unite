package com.app.prod.user.events;

import com.app.prod.eventbus.AppEvent;

import java.util.List;
import java.util.UUID;

/**
 * Sent after a manager invites people to a building - both freshly created accounts and ones that
 * are being invited again because the first mail never arrived.
 */
public record AccountsInvitedEvent(
        UUID buildingId,
        List<AccountInvitation> invitations
) implements AppEvent {
}
