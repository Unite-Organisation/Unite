package com.app.prod.post.dto;

import com.app.prod.post.enums.PostType;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.FilterRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class  PostFilterRequest extends FilterRequest {
    private UUID id;
    private PostType postType;
    private UUID createdBy;

    @DateTimeFormat(iso = DATE_TIME)
    private LocalDateTime visibleFrom;
    private ComparisonFilter.Modifier visibleFromModifier;

    @DateTimeFormat(iso = DATE_TIME)
    private LocalDateTime visibleTo;
    private ComparisonFilter.Modifier visibleToModifier;
}
