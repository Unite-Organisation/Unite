package com.app.prod.storage.handlers;

import com.app.prod.eventbus.AppEvent;

import java.util.List;

public record RemoveFilesEvent(List<String> keys) implements AppEvent {
}
