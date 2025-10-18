package com.app.prod.issues.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.issues.service.IssueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("issue")
@RequiredArgsConstructor
@Tag(name = "Issues")
public class IssueRestApi {

    private final IssueService issueService;
    private final GlobalSecurityManager globalSecurityManager;

    @PostMapping()
    public void createIssue(@RequestBody IssueRequest request){
        var user = globalSecurityManager.getCurrentUser();
        issueService.createIssue(request, user);
    }

}
