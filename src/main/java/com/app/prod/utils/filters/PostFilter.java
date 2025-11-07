package com.app.prod.utils.filters;

import com.app.prod.post.enums.PostType;
import lombok.Builder;
import org.jooq.Condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jooq.sources.Tables.POST;

@Builder
public class PostFilter implements PredicateFilter{
    Optional<PostType> postType;

    @Override
    public List<Condition> combineConditions() {
        List<Condition> conditionList = new ArrayList<>();

        postType.ifPresent(r -> conditionList.add(POST.POST_TYPE.eq(r.name())));

        return conditionList;
    }

}
