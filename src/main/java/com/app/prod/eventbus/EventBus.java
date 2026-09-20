package com.app.prod.eventbus;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EventBus {

    private final Map<Class<? extends AppEvent>, EventHandler<? extends AppEvent>> handlers;
    private final EventDispatcher eventDispatcher;

    public EventBus(List<EventHandler<? extends AppEvent>> handlers, EventDispatcher eventDispatcher) {
        this.handlers = indexByEventType(handlers);
        this.eventDispatcher = eventDispatcher;
        log.info("Event bus started with {} handlers", this.handlers.size());
    }

    public <E extends AppEvent> void publish(E event) {
        EventHandler<E> handler = resolveHandler(event);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            log.debug("Event {} published inside a transaction, deferring until commit", event.getClass().getSimpleName());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventDispatcher.dispatch(handler, event);
                }
            });
            return;
        }

        eventDispatcher.dispatch(handler, event);
    }

    @SuppressWarnings("unchecked")
    private <E extends AppEvent> EventHandler<E> resolveHandler(E event) {
        EventHandler<? extends AppEvent> handler = handlers.get(event.getClass());

        if (handler == null) {
            log.error("No handler registered for event {}", event.getClass().getSimpleName());
            throw new IllegalApplicationStateException(AppError.of(
                    Code.EVENT_HANDLER_NOT_FOUND,
                    String.format("No handler registered for event %s", event.getClass().getSimpleName())
            ));
        }

        return (EventHandler<E>) handler;
    }

    private static Map<Class<? extends AppEvent>, EventHandler<? extends AppEvent>> indexByEventType(
            List<EventHandler<? extends AppEvent>> handlers
    ) {
        Map<Class<? extends AppEvent>, EventHandler<? extends AppEvent>> byEventType = new HashMap<>();

        for (EventHandler<? extends AppEvent> handler : handlers) {
            EventHandler<? extends AppEvent> duplicate = byEventType.put(handler.eventType(), handler);

            if (duplicate != null) {
                throw new IllegalApplicationStateException(AppError.of(
                        Code.EVENT_HANDLER_DUPLICATED,
                        String.format("Event %s is handled by both %s and %s",
                                handler.eventType().getSimpleName(),
                                duplicate.getClass().getSimpleName(),
                                handler.getClass().getSimpleName())
                ));
            }
        }

        return Map.copyOf(byEventType);
    }
}
