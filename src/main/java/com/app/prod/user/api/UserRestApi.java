package com.app.prod.user.api;

import com.app.prod.user.dto.BulkCreationRequest;
import com.app.prod.user.dto.BulkCreationResponse;
import com.app.prod.user.dto.UserResponse;
import com.app.prod.user.mappers.UserMapper;
import com.app.prod.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.UsersRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
@Slf4j
public class UserRestApi {

    private final UserService userService;
    private final HandlerMapping resourceHandlerMapping;

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsers(){
        List<UsersRecord> users = userService.getUsers();
        return ResponseEntity.ok(UserMapper.fromRecordsToResponses(users));
    }

    @PostMapping("/bulk-creation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<BulkCreationResponse> bulkCreation(@RequestBody BulkCreationRequest request){
        BulkCreationResponse response = userService.bulkCreation(request);

        if(response.success()){
            return ResponseEntity.ok(response);
        }
        else{
            if(request.personToBeCreateds().size() != response.failedCreations().size()){
                log.warn("{} creations failed", response.failedCreations().size());
                return new ResponseEntity<>(HttpStatus.MULTI_STATUS);
            }
            else{
                log.error("All creations failed. Server error.");
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }


}
