package com.app.prod.cleaning.dtos;

import java.util.List;

public record DatabaseCleaningResponse(
        boolean dryRun,
        List<EntityCleaningResult> entityCleaningResults
) {
}
