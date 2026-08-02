package com.app.prod.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FileUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull Long size
) {
}
