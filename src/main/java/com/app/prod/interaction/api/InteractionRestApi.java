package com.app.prod.interaction.api;

import com.app.prod.config.security.GlobalSecurityManager;
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
    private final GlobalSecurityManager globalSecurityManager;

    @PostMapping()
    public void addInteraction(@Valid @RequestBody InteractionRequest request) {
        var userId = globalSecurityManager.getCurrentUser().getId();
        interactionService.addInteraction(userId, request);
    }

    @DeleteMapping()
    public void removeInteraction(
            @RequestParam InteractionEntityType entityType,
            @RequestParam UUID entityId,
            @RequestParam InteractionType interactionType
    ) {
        var userId = globalSecurityManager.getCurrentUser().getId();
        interactionService.removeInteraction(userId, entityType, entityId, interactionType);
    }


    @GetMapping()
    public List<InteractionUserResponse> getInteractions(
            @Valid @ModelAttribute Pagination pagination,
            @RequestParam InteractionEntityType entityType,
            @RequestParam UUID entityId,
            @RequestParam(required = false) InteractionType interactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) LocalDateTime createdAt,
            @RequestParam(required = false) ComparisonFilter.Modifier createdAtModifier
    ) {
        var userId = globalSecurityManager.getCurrentUser().getId();
        InteractionFilter filter = interactionFilteringService.prepareFilter(entityType, entityId, interactionType, createdAt, createdAtModifier);
        return interactionService.getInteractions(userId, entityType, entityId, filter, pagination);
    }
}
