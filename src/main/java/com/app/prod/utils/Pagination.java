package com.app.prod.utils;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record Pagination(
        @Min(5)
        @Max(100)
        Integer pageSize,
        @Min(1)
        Integer page
) {
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int DEFAULT_PAGE = 1;

    public Pagination {
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        page = page == null ? DEFAULT_PAGE : page;
    }

    public static Pagination of(Integer pageSize, Integer page) {
        return new Pagination(pageSize, page);
    }

    public static Pagination defaults() {
        return of(null, null);
    }

    public int getOffset(){
        return pageSize * (page  - 1);
    }
}
