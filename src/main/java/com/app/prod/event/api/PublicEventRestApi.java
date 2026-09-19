package com.app.prod.event.api;

import com.app.prod.event.dto.CreateEventRequest;
import com.app.prod.event.dto.EventCreatedResponse;
import com.app.prod.event.dto.EventResponse;
import com.app.prod.event.service.CreatedEvent;
import com.app.prod.event.service.EventSessionService;
import com.app.prod.event.service.PublicEventService;
import com.app.prod.event.web.EventCaller;
import com.app.prod.utils.ApplicationInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/event")
@RequiredArgsConstructor
@Tag(name = "Public events")
public class PublicEventRestApi {

    private static final String EVENT_PATH = "/public/event";

    private final PublicEventService publicEventService;
    private final ApplicationInfo applicationInfo;

    @GetMapping("/{slug}")
    public EventResponse getEvent(@PathVariable String slug) {
        return publicEventService.getEvent(slug);
    }

    @PostMapping()
    public ResponseEntity<EventCreatedResponse> createEvent(EventCaller caller, @Valid @RequestBody CreateEventRequest request, HttpServletRequest httpRequest) {
        CreatedEvent created = publicEventService.createEvent(request, caller);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionCookie(created.sessionToken(), httpRequest).toString())
                .body(new EventCreatedResponse(created.slug(), created.returnCode()));
    }

    private ResponseCookie sessionCookie(String sessionToken, HttpServletRequest httpRequest) {
        return ResponseCookie.from(EventSessionService.COOKIE_NAME, sessionToken)
                .httpOnly(true)
                .secure(applicationInfo.isProdEnvironment())
                .path(httpRequest.getContextPath() + EVENT_PATH)
                .maxAge(EventSessionService.SESSION_TTL)
                .sameSite("Lax")
                .build();
    }
}
