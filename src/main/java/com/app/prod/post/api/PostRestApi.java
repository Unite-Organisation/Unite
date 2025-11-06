package com.app.prod.post.api;

import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.AnnouncementResponse;
import com.app.prod.post.service.AnnouncementsService;
import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.utils.Pagination;
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

    private final AnnouncementsService announcementsService;
    private final GlobalSecurityManager globalSecurityManager;

    @PostMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UUID> createAnnouncement(@RequestBody AnnouncementRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        var id = announcementsService.createAnnouncement(request, userId);
        return ResponseEntity.ok(id);
    }

    @PatchMapping("/{id}/image")
    @PreAuthorize("hasRole('MANAGER')")
    public void addImageForAnnouncement(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile photo
    ){
        announcementsService.addImageForAnnouncement(photo, id);
    }

    @GetMapping()
    public List<AnnouncementResponse> getAnnouncements(@Valid @ModelAttribute Pagination pagination){
        var userId = globalSecurityManager.getCurrentUser().getId();
        return announcementsService.getAnnouncements(pagination, userId);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getAnnouncementPhoto(@PathVariable UUID id){
        var dataPair = announcementsService.getAnnouncementPhoto(id);

        return ResponseEntity.ok()
                .contentType(dataPair.getRight())
                .body(dataPair.getLeft());
    }

}
