package com.app.prod.event.api;

import com.app.prod.event.dto.*;
import com.app.prod.event.service.CreatedEvent;
import com.app.prod.event.service.EventMembershipService;
import com.app.prod.event.service.EventSessionService;
import com.app.prod.event.service.OpenedSession;
import com.app.prod.event.service.PublicEventService;
import com.app.prod.event.web.EventCaller;
import com.app.prod.event.web.EventScope;
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

import java.util.Optional;

@RestController
@RequestMapping("/public/event")
@RequiredArgsConstructor
@Tag(name = "Public events")
public class PublicEventRestApi {

    private static final String EVENT_PATH = "/public/event/";

    private final PublicEventService publicEventService;
    private final EventMembershipService eventMembershipService;
    private final ApplicationInfo applicationInfo;

    @PostMapping()
    public ResponseEntity<EventCreatedResponse> createEvent(EventCaller caller, @Valid @RequestBody CreateEventRequest request, HttpServletRequest httpRequest) {
        CreatedEvent created = publicEventService.createEvent(request, caller);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionCookie(created.slug(), created.sessionToken(), httpRequest).toString())
                .body(new EventCreatedResponse(created.slug(), created.returnCode()));
    }

    @GetMapping("/{slug}")
    public EventResponse getEvent(@PathVariable String slug, Optional<EventScope> scope) {
        return publicEventService.getEvent(slug, scope);
    }

    @PutMapping("/{slug}/attendance")
    public AttendanceResponse changeAttendance(EventScope scope, @Valid @RequestBody AttendanceRequest request) {
        return new AttendanceResponse(eventMembershipService.changeAttendance(scope, request.status()));
    }

    @PostMapping("/{slug}/session")
    public ResponseEntity<EventSessionResponse> openSession(
            @PathVariable String slug,
            EventCaller caller,
            @Valid @RequestBody(required = false) OpenSessionRequest request,
            HttpServletRequest httpRequest
    ) {
        OpenedSession opened = eventMembershipService.openSession(slug, caller, request);

        return ResponseEntity.status(opened.joined() ? HttpStatus.CREATED : HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, sessionCookie(slug, opened.sessionToken(), httpRequest).toString())
                .body(new EventSessionResponse(opened.member(), opened.returnCode()));
    }

    private ResponseCookie sessionCookie(String slug, String sessionToken, HttpServletRequest httpRequest) {
        return ResponseCookie.from(EventSessionService.COOKIE_NAME, sessionToken)
                .httpOnly(true)
                .secure(applicationInfo.isProdEnvironment())
                .path(httpRequest.getContextPath() + EVENT_PATH + slug)
                .maxAge(EventSessionService.SESSION_TTL)
                .sameSite("Lax")
                .build();
    }
}
