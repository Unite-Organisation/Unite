package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.post.dto.PostFilterRequest;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.Filter;
import com.app.prod.utils.filters.PostFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

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
                .buildingId(Filter.of(scope.buildingId()))
                .createdBy(Filter.of(request.getCreatedBy()))
                .postType(Filter.of(request.getPostType()))
                .attendedBy(Filter.of(request.getAttendedBy()))
                .visibleFrom(visibleFromFilter)
                .visibleTo(visibleToFilter)
                .build();
    }

}
