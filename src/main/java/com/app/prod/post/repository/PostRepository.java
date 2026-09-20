package com.app.prod.post.repository;

import com.app.prod.event.repository.EventMemberFields;
import com.app.prod.interaction.dto.InteractionSummary;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.repository.InteractionFields;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.enums.PostType;
import com.app.prod.storage.file.FileService;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.PostFilter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.sources.tables.Post;
import org.jooq.sources.tables.records.PostRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.coalesce;
import static org.jooq.sources.Tables.*;

@Repository
public class PostRepository extends BaseJooqRepository<Post, PostRecord, UUID> {

    private final FileService fileService;

    protected PostRepository(DSLContext dsl, FileService fileService) {
        super(dsl, POST, POST.ID);
        this.fileService = fileService;
    }

    public List<PostResponse> findPosts(UUID viewerId, Pagination pagination, PostFilter filter) {
        Field<List<InteractionSummary>> interactions = InteractionFields.summaryFor(POST.ID, InteractionEntityType.POST, viewerId);
        Field<Integer> attendeesCount = EventMemberFields.goingCount(POST.EVENT_ID).as("attendees_count");
        Field<String> name = coalesce(POST.NAME, EVENT.NAME).as("name");
        Field<String> content = coalesce(POST.CONTENT, EVENT.DESCRIPTION).as("content");

        return dslContext.select(
                        POST.ID,
                        name,
                        POST.BUILDING_ID,
                        POST.CREATED_BY,
                        content,
                        POST.RELATED_DATE,
                        POST.CREATED_AT,
                        POST.POST_TYPE,
                        EVENT.PUBLIC_SLUG,
                        EVENT.START_DATE_TIME,
                        EVENT.END_DATE_TIME,
                        EVENT.LOCATION_NAME,
                        EVENT.ONLINE_URL,
                        EVENT.MAX_ATTENDEES,
                        attendeesCount,
                        POST.VISIBLE_FROM,
                        POST.VISIBLE_TO,
                        POST.ATTACHMENTS,
                        interactions
                )
                .from(POST)
                .leftJoin(EVENT).on(EVENT.ID.eq(POST.EVENT_ID))
                .where(filter.parseFilter())
                .orderBy(POST.CREATED_AT)
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetch(record -> new PostResponse(
                        record.get(POST.ID),
                        record.get(name),
                        record.get(POST.BUILDING_ID),
                        record.get(POST.CREATED_BY),
                        record.get(content),
                        record.get(POST.RELATED_DATE),
                        record.get(POST.CREATED_AT),
                        PostType.valueOf(record.get(POST.POST_TYPE)),
                        record.get(EVENT.PUBLIC_SLUG),
                        record.get(EVENT.START_DATE_TIME),
                        record.get(EVENT.END_DATE_TIME),
                        record.get(EVENT.LOCATION_NAME),
                        record.get(EVENT.ONLINE_URL),
                        record.get(EVENT.MAX_ATTENDEES),
                        record.get(attendeesCount),
                        record.get(POST.VISIBLE_FROM),
                        record.get(POST.VISIBLE_TO),
                        fileService.toResponses(record.get(POST.ATTACHMENTS)),
                        record.get(interactions)
                ));
    }

    public boolean existsForEvent(UUID eventId) {
        return dslContext.fetchExists(POST, POST.EVENT_ID.eq(eventId));
    }

    public Optional<PostRecord> findInBuildingForUpdate(UUID buildingId, UUID postId) {
        return dslContext.selectFrom(POST)
                .where(POST.ID.eq(postId))
                .and(POST.BUILDING_ID.eq(buildingId))
                .forUpdate()
                .fetchOptional();
    }

    public Optional<PostType> findPostTypeInBuilding(UUID buildingId, UUID postId) {
        return dslContext.select(POST.POST_TYPE)
                .from(POST)
                .where(POST.ID.eq(postId))
                .and(POST.BUILDING_ID.eq(buildingId))
                .fetchOptional(record -> PostType.valueOf(record.get(POST.POST_TYPE)));
    }
}
