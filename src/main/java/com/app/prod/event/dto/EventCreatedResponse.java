package com.app.prod.event.dto;

/**
 * @param returnCode the guest's code for coming back on another device, {@code null} for a Unite account
 */
public record EventCreatedResponse(
        String slug,
        String returnCode
) {
}
