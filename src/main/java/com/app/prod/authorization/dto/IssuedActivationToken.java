package com.app.prod.authorization.dto;

import java.util.UUID;

public record IssuedActivationToken(
        UUID tokenId,
        UUID userId,
        String activationLink
) {
}
