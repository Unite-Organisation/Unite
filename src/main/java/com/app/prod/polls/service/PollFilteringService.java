package com.app.prod.polls.service;

import com.app.prod.polls.enums.PollStatus;
import com.app.prod.utils.filters.Filter;
import com.app.prod.utils.filters.PollFilter;
import org.springframework.stereotype.Service;


@Service
public class PollFilteringService {

    public PollFilter prepareFilter(
            PollStatus pollStatus
    ) {
        return PollFilter.builder()
                .pollStatus(Filter.of(pollStatus))
                .build();

    }
}
