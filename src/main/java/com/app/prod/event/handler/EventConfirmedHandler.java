package com.app.prod.event.handler;

import com.app.prod.event.events.EventConfirmedEvent;
import com.app.prod.eventbus.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventConfirmedHandler implements EventHandler<EventConfirmedEvent> {

    @Override
    public void handle(EventConfirmedEvent event) {
        log.info("NOTIFY: event {} confirmed on slot {} starting {}",
                event.eventId(), event.slotId(), event.startDateTime());
    }

    @Override
    public Class<EventConfirmedEvent> eventType() {
        return EventConfirmedEvent.class;
    }
}
