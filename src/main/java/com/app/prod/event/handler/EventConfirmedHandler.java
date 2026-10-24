package com.app.prod.event.handler;

import com.app.prod.event.events.EventConfirmedEvent;
import com.app.prod.event.service.EventMailService;
import com.app.prod.eventbus.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventConfirmedHandler implements EventHandler<EventConfirmedEvent> {

    private final EventMailService eventMailService;

    @Override
    public void handle(EventConfirmedEvent event) {
        log.info("Event {} confirmed on slot {} starting {}", event.eventId(), event.slotId(), event.startDateTime());
        eventMailService.confirmed(event.eventId(), event.slotId());
    }

    @Override
    public Class<EventConfirmedEvent> eventType() {
        return EventConfirmedEvent.class;
    }
}
