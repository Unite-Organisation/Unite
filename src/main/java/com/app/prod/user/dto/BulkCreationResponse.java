package com.app.prod.user.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record BulkCreationResponse(
        boolean success,
        List<Person> failedCreations
) {
}
