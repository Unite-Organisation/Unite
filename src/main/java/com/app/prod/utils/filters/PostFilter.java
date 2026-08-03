package com.app.prod.utils.filters;

import com.app.prod.post.enums.PostType;
import lombok.Builder;
import org.jooq.Condition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jooq.sources.Tables.POST;

@Builder
public class PostFilter implements PredicateFilter{
    Optional<PostType> postType;
    ComparisonFilter<LocalDateTime> visibleFrom;
    ComparisonFilter<LocalDateTime> visibleTo;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        postType.ifPresent(r -> conditionList.add(POST.POST_TYPE.eq(r.name())));
        visibleFrom.toCondition(POST.VISIBLE_FROM).ifPresent(conditionList::add);
        visibleTo.toCondition(POST.VISIBLE_TO).ifPresent(conditionList::add);

        return conditionList;
    }

}
