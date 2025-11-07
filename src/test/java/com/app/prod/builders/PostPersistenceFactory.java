package com.app.prod.builders;

import com.app.prod.post.enums.PostType;
import com.app.prod.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.PostRecord;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostPersistenceFactory {

    private final Clock clock;
    private final PostRepository announcementRepository;

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

        public Builder imageReference(String imageReference) {
            instance.setImageReference(imageReference);
            return this;
        }

        public Builder relatedDate(LocalDateTime relatedDate) {
            instance.setRelatedDate(relatedDate);
            return this;
        }

        public Builder withRandomValues() {
            instance.setId(UUID.randomUUID());
            instance.setName("Announcement-" + UUID.randomUUID().toString().substring(0, 5));
            instance.setCreatedAt(LocalDateTime.now(clock));
            instance.setContent("Sample content for announcement.");
            instance.setImageReference("https://example.com/image/" + UUID.randomUUID());
            instance.setRelatedDate(LocalDateTime.now(clock).plusDays(1));
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
