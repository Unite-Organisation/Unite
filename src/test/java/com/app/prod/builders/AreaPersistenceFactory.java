package com.app.prod.builders;
import com.app.prod.area.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.AreasRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AreaPersistenceFactory {

    private final Clock clock;
    private final AreaRepository areaRepository;

    public Builder getNewArea() {
        return new Builder();
    }

    public class Builder {

        private final AreasRecord instance;

        public Builder() {
            instance = new AreasRecord();
        }

        public Builder id(UUID id) {
            instance.setId(id);
            return this;
        }

        public Builder name(String name) {
            instance.setName(name);
            return this;
        }

        public Builder country(String country) {
            instance.setCountry(country);
            return this;
        }

        public Builder city(String city) {
            instance.setCity(city);
            return this;
        }

        public Builder type(String type) {
            instance.setType(type);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            instance.setCreatedAt(createdAt);
            return this;
        }

        public Builder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setName("Area-" + UUID.randomUUID().toString().substring(0, 5));
            instance.setCountry("Poland");
            instance.setCity("Krakow");
            instance.setType("Residential");
            instance.setCreatedAt(LocalDateTime.now(clock));
            return this;
        }

        public AreasRecord build() {
            return instance;
        }

        public AreasRecord buildAndSave() {
            AreasRecord record = build();
            areaRepository.insertOne(record);
            return record;
        }
    }
}