package com.app.prod.event.api;

import com.app.prod.event.dto.*;
import com.app.prod.event.service.*;
import com.app.prod.event.device.DeviceSignals;
import com.app.prod.event.device.DeviceSignalsFactory;
import com.app.prod.event.web.EventCaller;
import com.app.prod.event.web.RequestSignals;
import com.app.prod.event.web.EventScope;
import com.app.prod.utils.ApplicationInfo;
import com.app.prod.utils.CookieSession;
import com.app.prod.utils.filters.EventMemberFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final EventFilteringService eventFilteringService;
    private final DeviceSignalsFactory deviceSignalsFactory;

    @PostMapping()
    @CookieSession(
            value = CookieSession.Mode.NONE,
            note =  "Event can be created by: user with JWT (unite user) " +
                    "or by guest without any credentials"
    )
    public ResponseEntity<EventCreatedResponse> createEvent(EventCaller caller, @Valid @RequestBody CreateEventRequest request,
                                                           RequestSignals requestSignals, HttpServletRequest httpRequest) {
        CreatedEvent created = publicEventService.createEvent(request, caller, deviceSignalsFactory.from(requestSignals, request.device()));

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionCookie(created.slug(), created.sessionToken(), httpRequest).toString())
                .body(new EventCreatedResponse(created.slug(), created.returnCode()));
    }

    @GetMapping("/{slug}")
    @CookieSession(
            value = CookieSession.Mode.OPTIONAL,
            note =  "Event can be viewed by session authorized users (then response includes me field)" +
                    "or by guests that have got public link"
    )
    public EventResponse getEvent(@PathVariable String slug, Optional<EventScope> scope) {
        return publicEventService.getEvent(slug, scope);
    }

    @GetMapping("/{slug}/members")
    @CookieSession(
            value = CookieSession.Mode.NONE,
            note =  "The login screen shows this list before there is any session, so holding the link is enough"
    )
    public List<EventMemberResponse> getMembers(@PathVariable String slug, @Valid @ModelAttribute EventMembersRequest request, RequestSignals requestSignals) {
        EventMemberFilter filter = eventFilteringService.prepareFilter(request, slug);
        return eventMembershipService.findMembers(filter, request, deviceSignalsFactory.from(requestSignals, request.getDevice()));
    }

    @PutMapping("/{slug}/attendance")
    @CookieSession(
            value = CookieSession.Mode.REQUIRED,
            note =  "Changing attendance always requires session"
    )
    public AttendanceResponse changeAttendance(EventScope scope, @Valid @RequestBody AttendanceRequest request) {
        return new AttendanceResponse(eventMembershipService.changeAttendance(scope, request.status()));
    }

    @PostMapping("/{slug}/session")
    @CookieSession(value = CookieSession.Mode.NONE, note =  "Returns cookie")
    public ResponseEntity<EventSessionResponse> openSession(
            @PathVariable String slug,
            EventCaller caller,
            @Valid @RequestBody(required = false) OpenSessionRequest request,
            RequestSignals requestSignals,
            HttpServletRequest httpRequest
    ) {
        DeviceSignals device = deviceSignalsFactory.from(requestSignals, request == null ? null : request.device());
        OpenedSession opened = eventMembershipService.openSession(slug, caller, request, device);

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
