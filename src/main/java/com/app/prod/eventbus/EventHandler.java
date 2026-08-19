package com.app.prod.eventbus;


public interface EventHandler<E extends AppEvent> {

    void handle(E event);

    Class<E> eventType();
}
