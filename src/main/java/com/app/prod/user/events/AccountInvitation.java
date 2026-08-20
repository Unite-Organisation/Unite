package com.app.prod.user.events;

import java.util.UUID;

public record AccountInvitation(
        UUID userId,
        UUID tokenId,
        String email,
        String activationLink
) {
}
