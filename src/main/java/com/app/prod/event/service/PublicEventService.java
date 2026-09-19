package com.app.prod.event.service;

import com.app.prod.event.dto.EventResponse;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublicEventService {

    private final EventRepository eventRepository;

    public EventResponse getEvent(String slug) {
        return eventRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));
    }
}
