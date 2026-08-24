package com.app.prod.post.api;

import com.app.prod.access.BuildingScope;
import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.EventRequest;
import com.app.prod.post.dto.PostFilterRequest;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.service.AnnouncementService;
import com.app.prod.post.service.EventService;
import com.app.prod.post.service.PostFilteringService;
import com.app.prod.post.service.PostService;
import com.app.prod.utils.filters.PostFilter;
import com.app.prod.utils.shared.EntityCreatedResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PreAuthorize("hasAnyRole('MANAGER', 'RESIDENT', 'ADMIN')")
    public List<PostResponse> getPosts(BuildingScope scope, @Valid @ModelAttribute PostFilterRequest request){
        PostFilter filter = postFilteringService.prepareFilter(scope, request);
        return postService.getPosts(filter, request.pagination(), scope);
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

    @PutMapping("/announcement/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public PostResponse updateAnnouncement(BuildingScope scope, @Valid @RequestBody AnnouncementRequest request) {
        return announcementService.updateAnnouncement(scope, request);
    }

    @PutMapping("/event/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'RESIDENT')")
    public PostResponse updateEvent(BuildingScope scope, @Valid @RequestBody EventRequest request) {
        return eventService.updateEvent(scope, request);
    }

}
