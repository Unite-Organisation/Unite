package com.app.prod.post.service;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@NoArgsConstructor
final class VisibilityWindow {

    static void validate(LocalDateTime visibleFrom, LocalDateTime visibleTo) {
        if (visibleFrom == null || visibleTo == null) {
            return;
        }

        if (visibleFrom.isAfter(visibleTo)) {
            log.warn("Visible from: {} is after visible to: {}", visibleFrom, visibleTo);
            throw new BadRequestException(AppError.of(Code.INVALID_TIME_PERIOD, String.format("%s is after %s", visibleFrom, visibleTo)));
        }
    }

}
