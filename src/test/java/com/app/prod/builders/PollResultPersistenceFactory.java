package com.app.prod.builders;

import com.app.prod.polls.repository.PollResultRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.PollResultRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PollResultPersistenceFactory {

    private final Clock clock;
    private final PollResultRepository pollResultRepository;

    public Builder getNewPollResult() {
        return new Builder();
    }

    public class Builder {

        private final PollResultRecord instance;

        public Builder() {
            this.instance = new PollResultRecord();
        }

        public Builder id(UUID id) {
            instance.setId(id);
            return this;
        }

        public Builder pollId(UUID pollId) {
            instance.setPollId(pollId);
            return this;
        }

        public Builder chosenOption(UUID chosenOption) {
            instance.setChosenOption(chosenOption);
            return this;
        }

        public Builder votersCount(Integer votersCount) {
            instance.setVotersCount(votersCount);
            return this;
        }

        public Builder votingEnded(Boolean votingEnded) {
            instance.setVotingEnded(votingEnded);
            return this;
        }

        public Builder withRandomValues() {
            instance.setId(UUID.randomUUID());
            return this;
        }

        public PollResultRecord build() {
            return instance;
        }

        public PollResultRecord buildAndSave() {
            PollResultRecord record = build();
            pollResultRepository.insertOne(record);
            return record;
        }
    }
}