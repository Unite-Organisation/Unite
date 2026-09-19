package com.app.prod.event.api;

import com.app.prod.event.dto.EventResponse;
import com.app.prod.event.service.PublicEventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/event")
@RequiredArgsConstructor
@Tag(name = "Public events")
public class PublicEventRestApi {

    private final PublicEventService publicEventService;

    @GetMapping("/{slug}")
    public EventResponse getEvent(@PathVariable String slug) {
        return publicEventService.getEvent(slug);
    }
}
