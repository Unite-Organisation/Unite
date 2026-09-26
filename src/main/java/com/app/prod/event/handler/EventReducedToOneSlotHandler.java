package com.app.prod.event.handler;

import com.app.prod.event.events.EventReducedToOneSlotEvent;
import com.app.prod.eventbus.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventReducedToOneSlotHandler implements EventHandler<EventReducedToOneSlotEvent> {

    @Override
    public void handle(EventReducedToOneSlotEvent event) {
        log.info("NOTIFY: event {} reduced to one slot: {} after its deadline", event.eventId(), event.slotId());
    }

    @Override
    public Class<EventReducedToOneSlotEvent> eventType() {
        return EventReducedToOneSlotEvent.class;
    }
}
