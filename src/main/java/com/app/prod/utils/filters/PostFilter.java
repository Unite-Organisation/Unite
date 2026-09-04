package com.app.prod.utils.filters;

import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.interaction.repository.InteractionFields;
import com.app.prod.post.enums.PostType;
import lombok.Builder;
import org.jooq.Condition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.filters.Criteria.match;
import static com.app.prod.utils.filters.Criteria.matchEnum;
import static com.app.prod.utils.filters.Criteria.required;
import static com.app.prod.utils.filters.Criteria.when;
import static org.jooq.sources.Tables.POST;

@Builder
public class PostFilter implements PredicateFilter {
    Filter<UUID> id;
    Filter<UUID> buildingId;
    Filter<UUID> createdBy;
    Filter<PostType> postType;
    Filter<LocalDateTime> visibleFrom;
    Filter<LocalDateTime> visibleTo;
    Filter<UUID> attendedBy;

    @Override
    public List<Condition> combineConditions() {
        return Criteria.of(
                required(POST.BUILDING_ID, buildingId),
                match(POST.ID, id),
                match(POST.CREATED_BY, createdBy),
                matchEnum(POST.POST_TYPE, postType),
                match(POST.VISIBLE_FROM, visibleFrom),
                match(POST.VISIBLE_TO, visibleTo),
                when(attendedBy, userId -> InteractionFields.reactedBy(
                        POST.ID, InteractionEntityType.POST, InteractionType.ATTENDING, userId))
        );
    }

}
