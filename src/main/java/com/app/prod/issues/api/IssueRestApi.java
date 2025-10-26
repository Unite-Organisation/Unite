package com.app.prod.issues.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.issues.dto.IssueResponse;
import com.app.prod.issues.service.IssueFetchingService;
import com.app.prod.issues.service.IssueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("issue")
@RequiredArgsConstructor
@Tag(name = "Issues")
public class IssueRestApi {

    private final IssueService issueService;
    private final GlobalSecurityManager globalSecurityManager;
    private final IssueFetchingService issueFetchingService;

    @PostMapping()
    public void createIssue(@RequestBody IssueRequest request){
        var user = globalSecurityManager.getCurrentUser();
        issueService.createIssue(request, user);
    }

    @GetMapping("/building")
    public List<IssueResponse> getBuildingIssues(@RequestParam UUID buildingId){
        var user = globalSecurityManager.getCurrentUser();
        return issueFetchingService.getBuildingIssues(buildingId, user);
    }

    @GetMapping("/area")
    public List<IssueResponse> getAreaIssues(@RequestParam UUID areaId){
        var user = globalSecurityManager.getCurrentUser();
        return issueFetchingService.getAreaIssues(areaId, user);
    }

    @GetMapping("/facility")
    public List<IssueResponse> getFacilityIssues(@RequestParam UUID facilityId){
        var user = globalSecurityManager.getCurrentUser();
        return issueFetchingService.getFacilityIssues(facilityId, user);
    }

    @GetMapping("/poll")
    public List<IssueResponse> getPollIssues(@RequestParam UUID pollId){
        var user = globalSecurityManager.getCurrentUser();
        return issueFetchingService.getPollIssues(pollId, user);
    }

}
