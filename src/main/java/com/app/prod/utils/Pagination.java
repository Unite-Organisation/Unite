package com.app.prod.utils;

import jakarta.validation.constraints.Min;

public record Pagination(
        @Min(5)
        int pageSize,
        @Min(1)
        int page
) {
    public int getOffset(){
        return pageSize * (page  - 1);
    }
}
