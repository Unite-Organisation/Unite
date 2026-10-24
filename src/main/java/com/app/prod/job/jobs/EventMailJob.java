package com.app.prod.job.jobs;

import com.app.prod.job.JobRegistry;
import com.app.prod.mail.enums.EmailDeliveryType;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventMailJob(
        EmailDeliveryType type,
        UUID eventId,
        UUID slotId,
        LocalDateTime groupConfirmBy
) implements Job {

    @Override
    public void run(JobRegistry jobRegistry) {
        jobRegistry.sendEventMail(this);
    }
}
