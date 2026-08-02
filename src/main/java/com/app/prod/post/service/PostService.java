package com.app.prod.post.service;

import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.PostFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public List<PostResponse> getPosts(Pagination pagination, UUID userId, PostFilter filter) {
        return postRepository.findForUser(userId, pagination, filter);
    }
}
