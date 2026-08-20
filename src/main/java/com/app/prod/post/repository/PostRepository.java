package com.app.prod.post.repository;

import com.app.prod.interaction.dto.InteractionSummary;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
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

        return dslContext.select(
                        POST.ID,
                        POST.NAME,
                        POST.BUILDING_ID,
                        POST.CREATED_BY,
                        POST.CONTENT,
                        POST.RELATED_DATE,
                        POST.CREATED_AT,
                        POST.POST_TYPE,
                        POST.START_DATE_TIME,
                        POST.END_DATE_TIME,
                        POST.LOCATION_NAME,
                        POST.ONLINE_URL,
                        POST.MAX_ATTENDEES,
                        POST.VISIBLE_FROM,
                        POST.VISIBLE_TO,
                        POST.ATTACHMENTS,
                        interactions
                )
                .from(POST)
                .where(filter.parseFilter())
                .orderBy(POST.CREATED_AT)
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetch(record -> {
                    List<InteractionSummary> postInteractions = record.get(interactions);

                    return new PostResponse(
                        record.get(POST.ID),
                        record.get(POST.NAME),
                        record.get(POST.BUILDING_ID),
                        record.get(POST.CREATED_BY),
                        record.get(POST.CONTENT),
                        record.get(POST.RELATED_DATE),
                        record.get(POST.CREATED_AT),
                        PostType.valueOf(record.get(POST.POST_TYPE)),
                        record.get(POST.START_DATE_TIME),
                        record.get(POST.END_DATE_TIME),
                        record.get(POST.LOCATION_NAME),
                        record.get(POST.ONLINE_URL),
                        record.get(POST.MAX_ATTENDEES),
                        countOf(postInteractions, InteractionType.ATTENDING),
                        record.get(POST.VISIBLE_FROM),
                        record.get(POST.VISIBLE_TO),
                        fileService.toResponses(record.get(POST.ATTACHMENTS)),
                        postInteractions
                    );
                });
    }

    private static Integer countOf(List<InteractionSummary> interactions, InteractionType interactionType) {
        return interactions.stream()
                .filter(summary -> summary.interactionType() == interactionType)
                .map(InteractionSummary::count)
                .findFirst()
                .orElse(0);
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
