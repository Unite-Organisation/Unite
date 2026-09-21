package com.app.prod.utils.filters;

import com.app.prod.utils.Pagination;
import com.app.prod.utils.Search;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class FilterRequest {
    @Min(5)
    private Integer pageSize;
    @Min(1)
    private Integer page;
    private String search;

    public Pagination pagination() {
        return Pagination.of(pageSize, page);
    }

    public Search search() {
        return Search.of(search);
    }
}
