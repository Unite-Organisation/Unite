package com.app.prod.job.jobs;

import com.app.prod.config.SpringContextHolder;
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
        // add here more jobs in future
})
public interface Job extends Runnable{
    JobRegistry jobRegistry = SpringContextHolder.getBean(JobRegistry.class);
}
