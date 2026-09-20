package com.app.prod.eventbus;


public interface EventExecutor {
    void execute(Runnable task);
}
