package com.app.prod.job.jobs;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ConversationsCreateJob.class, name = "ConversationsCreateJob"),
        // add here more jobs in future
})
public interface Job extends Runnable{
}
