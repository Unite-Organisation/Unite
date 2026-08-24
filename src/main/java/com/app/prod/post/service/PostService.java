package com.app.prod.post.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.PostFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public List<PostResponse> getPosts(PostFilter filter, Pagination pagination, BuildingScope scope) {
        return postRepository.findPosts(scope.userId(), pagination, filter);
    }

    public PostResponse getPost(BuildingScope scope, UUID postId) {
        PostFilter filter = PostFilter.builder()
                .buildingId(scope.buildingId())
                .id(Optional.of(postId))
                .build();

        return getPosts(filter, null, scope).stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.POST_NOT_FOUND)));
    }
}
