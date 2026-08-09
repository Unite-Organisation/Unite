package com.app.prod.post.api;

import com.app.prod.access.BuildingScope;
import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.EventRequest;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.enums.PostType;
import com.app.prod.post.service.AnnouncementService;
import com.app.prod.post.service.EventService;
import com.app.prod.post.service.PostFilteringService;
import com.app.prod.post.service.PostService;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.ComparisonFilter;
import com.app.prod.utils.filters.PostFilter;
import com.app.prod.utils.shared.EntityCreatedResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@RestController
@RequestMapping("post")
@RequiredArgsConstructor
@Tag(name = "Posts")
public class PostRestApi {

    private final PostService postService;
    private final EventService eventService;
    private final AnnouncementService announcementService;
    private final PostFilteringService postFilteringService;

    @GetMapping()
    @PreAuthorize("hasAnyRole('MANAGER', 'RESIDENT')")
    public List<PostResponse> getPosts(
            BuildingScope scope,
            @Valid @ModelAttribute Pagination pagination,
            @RequestParam(required = false) PostType postType,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) LocalDateTime visibleFrom,
            @RequestParam(required = false) ComparisonFilter.Modifier visibleFromModifier,
            @RequestParam(required = false) @DateTimeFormat(iso = DATE_TIME) LocalDateTime visibleTo,
            @RequestParam(required = false) ComparisonFilter.Modifier visibleToModifier
    ){
        PostFilter filter = postFilteringService.prepareFilter(scope, postType, createdBy, visibleFrom, visibleFromModifier, visibleTo, visibleToModifier);
        return postService.getPosts(pagination, scope, filter);
    }

    @PostMapping("/announcement")
    @PreAuthorize("hasRole('MANAGER')")
    public EntityCreatedResponse createAnnouncement(BuildingScope scope, @Valid @RequestBody AnnouncementRequest request){
        return announcementService.createAnnouncement(request, scope);
    }

    @PostMapping("/event")
    @PreAuthorize("hasAnyRole('MANAGER', 'RESIDENT')")
    public EntityCreatedResponse createEvent(BuildingScope scope, @Valid @RequestBody EventRequest request){
        return eventService.createEvent(request, scope);
    }

}
