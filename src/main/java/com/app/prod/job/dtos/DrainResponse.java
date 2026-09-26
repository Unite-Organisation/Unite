package com.app.prod.job.dtos;

public record DrainResponse(
        int claimed,
        int succeeded,
        int failed
) {
    public static DrainResponse empty() {
        return new DrainResponse(0, 0, 0);
    }
}
