package com.app.prod.polls.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PollOptionPercentageShare(
        UUID optionId,
        BigDecimal percentageShare
) {
}
