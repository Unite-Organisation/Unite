package com.app.prod.utils.filters;

import com.app.prod.polls.enums.PollStatus;
import com.app.prod.post.enums.PostType;
import lombok.Builder;
import org.jooq.Condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jooq.sources.Tables.POLL;
import static org.jooq.sources.Tables.POST;

@Builder
public class PollFilter implements PredicateFilter{
    Optional<PollStatus> pollStatus;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        pollStatus.ifPresent(pollStatus -> {
            switch (pollStatus){
                case IN_PROGRESS -> conditionList.add(POLL.FINISHED.isFalse());
                case FINISHED -> conditionList.add(POLL.FINISHED.isTrue());
            }
        });

        return conditionList;
    }

}
