package com.app.prod.utils.filters;

import com.app.prod.post.enums.PostType;
import lombok.Builder;
import org.jooq.Condition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.POST;

@Builder
public class PostFilter implements PredicateFilter {
    UUID buildingId;
    @Builder.Default
    Optional<UUID> id = Optional.empty();
    @Builder.Default
    Optional<UUID> createdBy = Optional.empty();
    @Builder.Default
    Optional<PostType> postType = Optional.empty();
    @Builder.Default
    ComparisonFilter<LocalDateTime> visibleFrom = ComparisonFilter.empty();
    @Builder.Default
    ComparisonFilter<LocalDateTime> visibleTo = ComparisonFilter.empty();

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        conditionList.add(POST.BUILDING_ID.eq(buildingId));
        id.ifPresent(r -> conditionList.add(POST.ID.eq(r)));
        postType.ifPresent(r -> conditionList.add(POST.POST_TYPE.eq(r.name())));
        createdBy.ifPresent(userId -> conditionList.add(POST.CREATED_BY.eq(userId)));
        visibleFrom.toCondition(POST.VISIBLE_FROM).ifPresent(conditionList::add);
        visibleTo.toCondition(POST.VISIBLE_TO).ifPresent(conditionList::add);

        return conditionList;
    }

}
