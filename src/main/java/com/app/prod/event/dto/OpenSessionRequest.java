package com.app.prod.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OpenSessionRequest(
        @Size(max = 60) String displayName,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "\\d{4}") String returnCode,
        @Valid DeviceFingerprint device,
        @Valid List<SlotVoteRequest> votes
) {
}
