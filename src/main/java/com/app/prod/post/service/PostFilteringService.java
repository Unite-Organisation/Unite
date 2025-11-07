package com.app.prod.post.service;

import com.app.prod.post.enums.PostType;
import com.app.prod.utils.filters.PostFilter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PostFilteringService {

    public PostFilter prepareFilter(
            PostType postType
    ) {
        return PostFilter.builder()
                .postType(Optional.ofNullable(postType))
                .build();
    }

}
