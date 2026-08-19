package com.app.prod.user.events;

import com.app.prod.eventbus.AppEvent;

import java.util.List;
import java.util.UUID;

public record PendingAccountsCreatedEvent(
        UUID buildingId,
        List<AccountInvitation> invitations
) implements AppEvent {
}
