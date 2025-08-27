package com.app.prod.user.dto;

import java.util.List;

public record BulkCreationRequest(
        List<Person> persons
) {
}
