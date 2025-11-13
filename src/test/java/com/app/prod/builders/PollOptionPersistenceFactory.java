package com.app.prod.builders;

import com.app.prod.polls.repository.PollOptionRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.PollOptionRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PollOptionPersistenceFactory {

    private final Clock clock;
    private final PollOptionRepository pollOptionRepository;

    public Builder getNewPollOption() {
        return new Builder();
    }

    public class Builder {

        private final PollOptionRecord instance;

        public Builder() {
            instance = new PollOptionRecord();
        }

        public Builder id(UUID id) {
            instance.setId(id);
            return this;
        }

        public Builder pollId(UUID pollId) {
            instance.setPollId(pollId);
            return this;
        }

        public Builder optionText(String optionText) {
            instance.setOptionText(optionText);
            return this;
        }

        public Builder optionVotes(int optionVotes) {
            instance.setOptionVotes(optionVotes);
            return this;
        }

        public Builder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setOptionText(UUID.randomUUID().toString().substring(0, 12));
            instance.setOptionVotes(0);
            return this;
        }

        public PollOptionRecord build() {
            return instance;
        }

        public PollOptionRecord buildAndSave() {
            PollOptionRecord record = build();
            pollOptionRepository.insertOne(record);
            return record;
        }
    }
}
