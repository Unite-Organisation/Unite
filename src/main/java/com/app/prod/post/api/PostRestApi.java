package com.app.prod.post.api;

import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.EventRequest;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.enums.PostType;
import com.app.prod.post.service.AnnouncementService;
import com.app.prod.post.service.EventService;
import com.app.prod.post.service.PostFilteringService;
import com.app.prod.post.service.PostService;
import com.app.prod.config.security.GlobalSecurityManager;
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

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@RestController
@RequestMapping("post")
@RequiredArgsConstructor
@Tag(name = "Posts")
public class PostRestApi {

    private final PostService postService;
    private final GlobalSecurityManager globalSecurityManager;
    private final EventService eventService;
    private final AnnouncementService announcementService;
    private final PostFilteringService postFilteringService;

    @GetMapping()
    public List<PostResponse> getPosts(
            @Valid @ModelAttribute Pagination pagination,
            @RequestParam(required = false) PostType postType,
            @RequestParam @DateTimeFormat(iso = DATE_TIME) LocalDateTime visibleFrom,
            @RequestParam ComparisonFilter.Modifier visibleFromModifier,
            @RequestParam @DateTimeFormat(iso = DATE_TIME) LocalDateTime visibleTo,
            @RequestParam ComparisonFilter.Modifier visibleToModifier
    ){
        var userId = globalSecurityManager.getCurrentUser().getId();
        PostFilter filter = postFilteringService.prepareFilter(postType, visibleFrom, visibleFromModifier, visibleTo, visibleToModifier);
        return postService.getPosts(pagination, userId, filter);
    }

    /* Announcements */
    @PostMapping("/announcement")
    @PreAuthorize("hasRole('MANAGER')")
    public EntityCreatedResponse createAnnouncement(@Valid @RequestBody AnnouncementRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        return announcementService.createAnnouncement(request, userId);
    }

    /* Events */
    @PostMapping("/event")
    @PreAuthorize("hasAnyRole('MANAGER', 'RESIDENT')")
    public EntityCreatedResponse createEvent(@Valid @RequestBody EventRequest request){
        var user = globalSecurityManager.getCurrentUser();
        return eventService.createEvent(request, user);
    }

}
