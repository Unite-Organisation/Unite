package com.app.prod.eventbus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatcher {

    private final EventExecutor eventExecutor;

    public <E extends AppEvent> void dispatch(EventHandler<E> handler, E event) {
        UUID eventId = UUID.randomUUID();
        String eventName = event.getClass().getSimpleName();

        log.info("Dispatching event {} [{}]", eventName, eventId);
        eventExecutor.execute(() -> handle(handler, event, eventName, eventId));
    }

    private <E extends AppEvent> void handle(EventHandler<E> handler, E event, String eventName, UUID eventId) {
        try {
            handler.handle(event);
            log.info("Event {} [{}] handled successfully", eventName, eventId);
        } catch (Exception e) {
            log.error("Event {} [{}] failed and will not be retried", eventName, eventId, e);
        }
    }
}
