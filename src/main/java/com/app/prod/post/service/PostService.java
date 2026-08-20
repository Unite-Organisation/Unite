package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.PostFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public List<PostResponse> getPosts(Pagination pagination, BuildingScope scope, PostFilter filter) {
        return postRepository.findPosts(scope.userId(), pagination, filter);
    }
}
