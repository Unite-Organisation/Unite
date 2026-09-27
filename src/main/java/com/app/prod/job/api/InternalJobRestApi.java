package com.app.prod.job.api;

import com.app.prod.job.dtos.DrainResponse;
import com.app.prod.job.scheduled.ScheduledJobDrainer;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Machine to machine, not a user endpoint: authorized by the drain token, never by a JWT.
 * Cloud Scheduler calls it on a cron; calling it by hand is the way to push a stuck queue.
 */
@RestController
@RequestMapping("/internal/jobs")
@RequiredArgsConstructor
@Tag(name = "Internal jobs")
public class InternalJobRestApi {

    private final ScheduledJobDrainer scheduledJobDrainer;
    private final Clock clock;

    @PostMapping("/drain")
    public DrainResponse drain() {
        return scheduledJobDrainer.drainDue(LocalDateTime.now(clock));
    }
}
