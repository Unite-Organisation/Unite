package com.app.prod.event.handler;

import com.app.prod.event.events.EventGroupBrokeUpEvent;
import com.app.prod.event.service.EventMailService;
import com.app.prod.eventbus.EventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventGroupBrokeUpHandler implements EventHandler<EventGroupBrokeUpEvent> {

    private final EventMailService eventMailService;

    @Override
    public void handle(EventGroupBrokeUpEvent event) {
        log.info("Event {} lost its group on slot {} before {} and is collecting votes again", event.eventId(), event.slotId(), event.confirmBy());
        eventMailService.groupBrokeUp(event.eventId(), event.slotId(), event.confirmBy());
    }

    @Override
    public Class<EventGroupBrokeUpEvent> eventType() {
        return EventGroupBrokeUpEvent.class;
    }
}
