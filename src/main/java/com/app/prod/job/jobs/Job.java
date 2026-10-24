package com.app.prod.job.jobs;

import com.app.prod.job.JobRegistry;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConversationsCreateJob.class, name = "ConversationsCreateJob"),
        @JsonSubTypes.Type(value = SyncUserJob.class, name = "SyncUserJob"),
        @JsonSubTypes.Type(value = NoOpJob.class, name = "NoOpJob"),
        @JsonSubTypes.Type(value = EventDeadlineJob.class, name = "EventDeadlineJob"),
        @JsonSubTypes.Type(value = EventMailJob.class, name = "EventMailJob"),
        // add here more jobs in future
})
public interface Job {

    /**
     * @param jobRegistry handed in by whoever runs the job, from its own Spring context. A job is
     *                    plain data read back from JSON, so it has no way to hold beans of its own -
     *                    and a static lookup would run it against whichever context was set last.
     */
    void run(JobRegistry jobRegistry);
}
