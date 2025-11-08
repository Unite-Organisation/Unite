package com.app.prod.builders;

import com.app.prod.polls.repository.PollVotesRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.PollVoteRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PollVotesPersistenceFactory {

    private final Clock clock;
    private final PollVotesRepository pollVotesRepository;

    public Builder getNewPollVote() {
        return new Builder();
    }

    public class Builder {

        private final PollVoteRecord instance;

        public Builder() {
            this.instance = new PollVoteRecord();
        }

        public Builder id(UUID id) {
            instance.setId(id);
            return this;
        }

        public Builder pollId(UUID pollId) {
            instance.setPollId(pollId);
            return this;
        }

        public Builder optionId(UUID optionId) {
            instance.setOptionId(optionId);
            return this;
        }

        public Builder userId(UUID userId) {
            instance.setUserId(userId);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            instance.setCreatedAt(createdAt);
            return this;
        }

        public Builder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setCreatedAt(LocalDateTime.now(clock));
            return this;
        }

        public PollVoteRecord build() {
            return instance;
        }

        public PollVoteRecord buildAndSave() {
            PollVoteRecord record = build();
            pollVotesRepository.insertOne(record);
            return record;
        }
    }
}