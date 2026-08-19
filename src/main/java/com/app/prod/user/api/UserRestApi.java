package com.app.prod.user.api;

import com.app.prod.access.BuildingScope;
import com.app.prod.building.dto.HomePageResponse;
import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.user.dto.*;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.service.PendingAccountService;
import com.app.prod.user.service.UserCommunityService;
import com.app.prod.user.service.UserService;
import com.app.prod.utils.Pagination;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserRestApi {

    private final UserService userService;
    private final UserCommunityService userCommunityService;
    private final PendingAccountService pendingAccountService;
    private final GlobalSecurityManager globalSecurityManager;

    @GetMapping("meta-info")
    public UserMetaInfo getUserMetaData() {
        var user = globalSecurityManager.getCurrentUser();
        return userService.getUserMetadata(user);
    }

    @GetMapping("/my-data")
    public HomePageResponse getUserData(){
        var user = globalSecurityManager.getCurrentUser();
        return userCommunityService.getData(user);
    }

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsers(){
        List<AppUserRecord> users = userService.getUsers();
        return ResponseEntity.ok(UserMapper.fromRecordsToResponses(users));
    }

    @GetMapping("/in-area")
    public List<PotentialContactResponse> getUsersInMyArea(
            @Valid @ModelAttribute Pagination pagination){
        var user = globalSecurityManager.getCurrentUser();
        return userCommunityService.getAllUsersInArea(user, pagination);
    }

    @GetMapping("/to-add")
    @PreAuthorize("hasRole('MANAGER')")
    public List<ResidentToAdd> getAllUsersWithoutBuildings(){
        return userService.getUsersWithoutBuilding();
    }

    @PostMapping("/bulk-creation")
    @PreAuthorize("hasRole('MANAGER')")
    public BulkCreationResponse bulkCreation(BuildingScope scope, @Valid @RequestBody BulkCreationRequest request){
        return pendingAccountService.createPendingAccounts(scope, request);
    }

}
