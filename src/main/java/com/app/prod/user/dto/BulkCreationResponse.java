package com.app.prod.user.dto;

import java.util.List;

public record BulkCreationResponse(
        List<CreatedAccount> created,
        List<ReinvitedAccount> reinvited,
        List<SkippedEmail> skipped
) {
}
