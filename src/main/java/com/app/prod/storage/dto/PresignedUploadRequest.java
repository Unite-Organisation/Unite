package com.app.prod.storage.dto;

import com.app.prod.storage.ContextStoragePrefix;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PresignedUploadRequest(
        @NotNull ContextStoragePrefix context,
        @NotEmpty @Valid List<FileUploadRequest> files
) {
}
