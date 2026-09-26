package com.app.prod.event.handler;

import com.app.prod.event.events.EventGroupFormedEvent;
import com.app.prod.eventbus.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventGroupFormedHandler implements EventHandler<EventGroupFormedEvent> {

    @Override
    public void handle(EventGroupFormedEvent event) {
        log.info("NOTIFY: event {} formed on slot {}, members may walk out until {}",
                event.eventId(), event.slotId(), event.confirmBy());
    }

    @Override
    public Class<EventGroupFormedEvent> eventType() {
        return EventGroupFormedEvent.class;
    }
}
