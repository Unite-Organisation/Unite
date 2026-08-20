package com.app.prod.eventbus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VirtualThreadEventExecutor implements EventExecutor, DisposableBean {

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void destroy() throws InterruptedException {
        log.info("Shutting down event executor, waiting up to {}s for events in flight", SHUTDOWN_TIMEOUT_SECONDS);
        executor.shutdown();

        if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            log.warn("Event executor did not finish within {}s, dropping remaining events", SHUTDOWN_TIMEOUT_SECONDS);
            executor.shutdownNow();
            return;
        }

        log.info("Event executor shut down cleanly");
    }
}
