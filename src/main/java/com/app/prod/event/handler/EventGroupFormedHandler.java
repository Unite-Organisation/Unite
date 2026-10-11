package com.app.prod.event.handler;

import com.app.prod.event.events.EventGroupFormedEvent;
import com.app.prod.event.service.EventMailService;
import com.app.prod.eventbus.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventGroupFormedHandler implements EventHandler<EventGroupFormedEvent> {

    private final EventMailService eventMailService;

    @Override
    public void handle(EventGroupFormedEvent event) {
        log.info("Event {} formed on slot {}, members may walk out until {}", event.eventId(), event.slotId(), event.confirmBy());
        eventMailService.groupFormed(event.eventId(), event.slotId(), event.confirmBy());
    }

    @Override
    public Class<EventGroupFormedEvent> eventType() {
        return EventGroupFormedEvent.class;
    }
}
