package com.app.prod.storage.handlers;

import com.app.prod.eventbus.EventHandler;
import com.app.prod.storage.file.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RemoveFilesEventHandler implements EventHandler<RemoveFilesEvent> {

    private final FileService fileService;

    @Override
    public void handle(RemoveFilesEvent event) {
        for (String key : event.keys()) {
            log.info("Removing file {}", key);
            fileService.safeDelete(key);
        }
    }

    @Override
    public Class<RemoveFilesEvent> eventType() {
        return RemoveFilesEvent.class;
    }
}
