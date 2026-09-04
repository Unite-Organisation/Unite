package com.app.prod.utils.filters;

import com.app.prod.polls.enums.PollStatus;
import lombok.Builder;
import org.jooq.Condition;

import java.util.List;

import static com.app.prod.utils.filters.Criteria.when;
import static org.jooq.sources.Tables.POLL;

@Builder
public class PollFilter implements PredicateFilter {
    Filter<PollStatus> pollStatus;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                when(pollStatus, status -> POLL.FINISHED.eq(status == PollStatus.FINISHED))
        );
    }

}
