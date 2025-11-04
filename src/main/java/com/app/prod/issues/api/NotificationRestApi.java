package com.app.prod.issues.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.issues.dto.NotificationResponse;
import com.app.prod.issues.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("notification")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationRestApi {

    private final GlobalSecurityManager globalSecurityManager;
    private final NotificationService notificationService;

    @GetMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public List<NotificationResponse> getManagersNotifications(){
        var managerId = globalSecurityManager.getCurrentUser().getId();
        return notificationService.getNotifications(managerId);
    }

    @PatchMapping("/{notificationId}/seen")
    @PreAuthorize("hasRole('MANAGER')")
    public void notificationViewed(@PathVariable UUID notificationId){
        var managerId = globalSecurityManager.getCurrentUser().getId();
        notificationService.updateNotificationSeenAtDate(managerId, notificationId);
    }

}
