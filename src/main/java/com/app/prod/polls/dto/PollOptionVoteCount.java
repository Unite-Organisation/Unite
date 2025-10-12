package com.app.prod.polls.dto;

import java.util.UUID;

public record PollOptionVoteCount (
        UUID optionId,
        Integer count,
        String content
){
}
