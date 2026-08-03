package com.app.prod.post.service;

import com.app.prod.post.enums.PostType;
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

    public PostFilter prepareFilter(
            PostType postType,
            LocalDateTime visibleFrom, ComparisonFilter.Modifier visibleFromModifier,
            LocalDateTime visibleTo, ComparisonFilter.Modifier visibleToModifier
    ) {

        ComparisonFilter<LocalDateTime> visibleToFilter = ComparisonFilter.of(visibleTo, visibleToModifier);
        if (visibleToFilter.isEmpty()) {
            visibleToFilter = ComparisonFilter.of(LocalDateTime.now(clock), ComparisonFilter.Modifier.GREATER_OR_EQUAL_THAN);
        }

        ComparisonFilter<LocalDateTime> visibleFromFilter = ComparisonFilter.of(visibleFrom, visibleFromModifier);
        if (visibleFromFilter.isEmpty()) {
            visibleFromFilter = ComparisonFilter.of(LocalDateTime.now(clock), ComparisonFilter.Modifier.LESS_OR_EQUAL_THAN);
        }

        return PostFilter.builder()
                .postType(Optional.ofNullable(postType))
                .visibleFrom(visibleFromFilter)
                .visibleTo(visibleToFilter)
                .build();
    }

}
