package com.app.prod.announcements.api;

import com.app.prod.announcements.dto.AnnouncementRequest;
import com.app.prod.announcements.dto.AnnouncementResponse;
import com.app.prod.announcements.service.AnnouncementsService;
import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.utils.Pagination;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("announcements")
@RequiredArgsConstructor
public class AnnouncementsRestApi {

    private final AnnouncementsService announcementsService;
    private final GlobalSecurityManager globalSecurityManager;

    @PostMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public void createAnnouncement(@RequestBody AnnouncementRequest request){
        var userId = globalSecurityManager.getCurrentUser().getId();
        announcementsService.createAnnouncement(request, userId);
    }

    @GetMapping()
    public List<AnnouncementResponse> getAnnouncements(@Valid @RequestParam Pagination pagination){
        var userId = globalSecurityManager.getCurrentUser().getId();
        return announcementsService.getAnnouncements(pagination, userId);
    }

}
