package com.app.prod.event.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventSlotResponse(
        UUID id,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int preferredCount,
        int ifNeededCount,
        int chance,
        boolean open
) {
}
