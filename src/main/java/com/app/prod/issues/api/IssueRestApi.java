package com.app.prod.issues.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.issues.dto.IssueRequest;
import com.app.prod.issues.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("issue")
@RequiredArgsConstructor
public class IssueRestApi {

    private final IssueService issueService;
    private final GlobalSecurityManager globalSecurityManager;

    @PostMapping()
    public void createIssue(@RequestBody IssueRequest request){
        var user = globalSecurityManager.getCurrentUser();
        issueService.createIssue(request, user);
    }

}
