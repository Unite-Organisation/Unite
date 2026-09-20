package com.app.prod.storage.api;

import com.app.prod.config.security.GlobalSecurityManager;
import com.app.prod.storage.dto.PresignedUpload;
import com.app.prod.storage.dto.PresignedUploadRequest;
import com.app.prod.storage.file.FileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("file")
@RequiredArgsConstructor
@Tag(name = "Files")
public class FileRestApi {

    private final FileService fileService;
    private final GlobalSecurityManager globalSecurityManager;

    @PostMapping("/presigned-upload")
    @PreAuthorize("hasAnyRole('MANAGER', 'RESIDENT')")
    public List<PresignedUpload> createUploadUrls(@Valid @RequestBody PresignedUploadRequest request) {
        var userId = globalSecurityManager.getCurrentUser().getId();
        return fileService.createUploadUrls(request.context(), userId, request.files());
    }
}
