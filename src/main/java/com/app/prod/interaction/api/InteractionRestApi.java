package com.app.prod.interaction.api;

import com.app.prod.access.BuildingScope;
import com.app.prod.interaction.dto.InteractionRequest;
import com.app.prod.interaction.dto.InteractionUserResponse;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.interaction.service.InteractionFilteringService;
import com.app.prod.interaction.service.InteractionService;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.InteractionFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@RestController
@RequestMapping("interaction")
@RequiredArgsConstructor
@Tag(name = "Interactions")
public class InteractionRestApi {

    private final InteractionService interactionService;
    private final InteractionFilteringService interactionFilteringService;

    @PostMapping()
    public void addInteraction(BuildingScope scope, @Valid @RequestBody InteractionRequest request) {
        interactionService.addInteraction(scope, request);
    }

    @DeleteMapping()
    public void removeInteraction(
            BuildingScope scope,
            @RequestParam InteractionEntityType entityType,
            @RequestParam UUID entityId,
            @RequestParam InteractionType interactionType
    ) {
        interactionService.removeInteraction(scope, entityType, entityId, interactionType);
    }


    @GetMapping()
    public List<InteractionUserResponse> getInteractions(
            BuildingScope scope,
            @Valid @ModelAttribute Pagination pagination,
            @RequestParam InteractionEntityType entityType,
            @RequestParam UUID entityId,
            @RequestParam(required = false) InteractionType interactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) LocalDateTime createdAt,
            @RequestParam(required = false) ComparisonFilter.Modifier createdAtModifier
    ) {
        InteractionFilter filter = interactionFilteringService.prepareFilter(entityType, entityId, interactionType, createdAt, createdAtModifier);
        return interactionService.getInteractions(scope, entityType, entityId, filter, pagination);
    }
}
