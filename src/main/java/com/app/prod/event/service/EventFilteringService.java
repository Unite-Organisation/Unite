package com.app.prod.event.service;

import com.app.prod.event.dto.EventMembersRequest;
import com.app.prod.event.repository.EventRepository;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.utils.filters.EventMemberFilter;
import com.app.prod.utils.filters.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventFilteringService {

    private final EventRepository eventRepository;

    public EventMemberFilter prepareFilter(EventMembersRequest request, String slug) {
        UUID eventId = eventRepository.findIdBySlug(slug)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.EVENT_NOT_FOUND)));

        return EventMemberFilter.builder()
                .eventId(Filter.of(eventId))
                .role(Filter.of(request.getRole()))
                .status(Filter.of(request.getStatus()))
                .build();
    }
}
