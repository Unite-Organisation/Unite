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
import com.app.prod.utils.filters.PostFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

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
            @RequestParam(required = false) PostType postType
    ){
        var userId = globalSecurityManager.getCurrentUser().getId();
        PostFilter filter = postFilteringService.prepareFilter(postType);
        return postService.getPosts(pagination, userId, filter);
    }

    @PatchMapping("/{id}/image")
    @PreAuthorize("hasRole('MANAGER')")
    public void addImageForPost(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile photo
    ){
        postService.addImageForPost(photo, id);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getPostPhoto(@PathVariable UUID id){
        var dataPair = postService.getPostPhoto(id);

        return ResponseEntity.ok()
                .contentType(dataPair.getRight())
                .body(dataPair.getLeft());
    }

    /* Announcements */
    @PostMapping("/announcement")
    @PreAuthorize("hasRole('MANAGER')")
    public void createAnnouncement(@RequestBody AnnouncementRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        announcementService.createAnnouncement(request, userId);
    }

    /* Events */
    @PostMapping("/event")
    @PreAuthorize("hasAnyRole('MANAGER', 'RESIDENT')")
    public void createEvent(@RequestBody EventRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        eventService.createEvent(request, userId);
    }

}
