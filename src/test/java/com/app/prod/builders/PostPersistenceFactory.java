package com.app.prod.builders;

import com.app.prod.post.enums.PostType;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.storage.dto.StoredFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jooq.JSONB;
import org.jooq.sources.tables.records.PostRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostPersistenceFactory {

    private static final JSONB EMPTY_ATTACHMENTS = JSONB.valueOf("[]");

    private final Clock clock;
    private final PostRepository announcementRepository;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    private String serialize(List<StoredFile> files) {
        return objectMapper.writeValueAsString(files);
    }

    public Builder getNewPost(PostType postType) {
        return new Builder(postType);
    }

    public class Builder {

        private final PostRecord instance;

        public Builder(PostType postType) {
            instance = new PostRecord();
            instance.setPostType(postType.name());
        }

        public Builder id(UUID id) {
            instance.setId(id);
            return this;
        }

        public Builder name(String name) {
            instance.setName(name);
            return this;
        }

        public Builder areaId(UUID areaId) {
            instance.setAreaId(areaId);
            return this;
        }

        public Builder buildingId(UUID buildingId) {
            instance.setBuildingId(buildingId);
            return this;
        }

        public Builder createdBy(UUID createdBy) {
            instance.setCreatedBy(createdBy);
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            instance.setCreatedAt(createdAt);
            return this;
        }

        public Builder content(String content) {
            instance.setContent(content);
            return this;
        }

        public Builder attachments(JSONB attachments) {
            instance.setAttachments(attachments);
            return this;
        }

        public Builder attachments(StoredFile... files) {
            return attachments(JSONB.valueOf(serialize(List.of(files))));
        }

        public Builder relatedDate(LocalDateTime relatedDate) {
            instance.setRelatedDate(relatedDate);
            return this;
        }

        public Builder visibleFrom(LocalDateTime visibleFrom) {
            instance.setVisibleFrom(visibleFrom);
            return this;
        }

        public Builder visibleTo(LocalDateTime visibleTo) {
            instance.setVisibleTo(visibleTo);
            return this;
        }

        public Builder startDateTime(LocalDateTime startDateTime) {
            instance.setStartDateTime(startDateTime);
            return this;
        }

        public Builder endDateTime(LocalDateTime endDateTime) {
            instance.setEndDateTime(endDateTime);
            return this;
        }

        public Builder maxAttendees(Integer maxAttendees) {
            instance.setMaxAttendees(maxAttendees);
            return this;
        }

        public Builder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setName("Announcement-" + UUID.randomUUID().toString().substring(0, 5));
            instance.setCreatedAt(LocalDateTime.now(clock));
            instance.setContent("Sample content for announcement.");
            instance.setAttachments(EMPTY_ATTACHMENTS);
            instance.setRelatedDate(LocalDateTime.now(clock).plusDays(1));
            instance.setVisibleFrom(LocalDateTime.now(clock).minusDays(100));
            instance.setVisibleTo(LocalDateTime.now(clock).plusDays(100));
            return this;
        }

        public PostRecord build() {
            return instance;
        }

        public PostRecord buildAndSave() {
            PostRecord record = build();
            announcementRepository.insertOne(record);
            return record;
        }
    }
}
