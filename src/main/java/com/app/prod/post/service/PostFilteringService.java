package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.post.dto.PostFilterRequest;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.PostFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostFilteringService {

    private final Clock clock;

    public PostFilter prepareFilter(BuildingScope scope, PostFilterRequest request) {

        ComparisonFilter<LocalDateTime> visibleToFilter = ComparisonFilter.of(request.getVisibleTo(), request.getVisibleToModifier());
        if (visibleToFilter.isEmpty()) {
            visibleToFilter = ComparisonFilter.of(LocalDateTime.now(clock), ComparisonFilter.Modifier.GREATER_OR_EQUAL_THAN);
        }

        ComparisonFilter<LocalDateTime> visibleFromFilter = ComparisonFilter.of(request.getVisibleFrom(), request.getVisibleFromModifier());
        if (visibleFromFilter.isEmpty()) {
            visibleFromFilter = ComparisonFilter.of(LocalDateTime.now(clock), ComparisonFilter.Modifier.LESS_OR_EQUAL_THAN);
        }

        return PostFilter.builder()
                .buildingId(scope.buildingId())
                .createdBy(Optional.ofNullable(request.getCreatedBy()))
                .postType(Optional.ofNullable(request.getPostType()))
                .visibleFrom(visibleFromFilter)
                .visibleTo(visibleToFilter)
                .build();
    }

}
