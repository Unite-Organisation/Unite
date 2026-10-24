package com.app.prod.event.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record EventMemberIdsRequest(
        @NotEmpty @Size(max = 100) List<@NotNull UUID> memberIds
) {
}
