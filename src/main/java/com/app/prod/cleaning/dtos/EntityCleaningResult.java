package com.app.prod.cleaning.dtos;


public record EntityCleaningResult(
        String entityName,
        int cleanedEntityCount,
        boolean failed
) {
}
