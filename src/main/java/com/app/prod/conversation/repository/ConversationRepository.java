package com.app.prod.conversation.repository;

import com.app.prod.conversation.dto.ConversationResponse;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.Pagination;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.sources.tables.Conversation;
import org.jooq.sources.tables.records.ConversationRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.*;
import static org.jooq.sources.Tables.*;

@Repository
public class ConversationRepository extends BaseJooqRepository<Conversation, ConversationRecord, UUID> {
    protected ConversationRepository(DSLContext dsl) {
        super(dsl, CONVERSATION, CONVERSATION.ID);
    }

    public Optional<UUID> findDirectConversationForUsers(UUID user1Id, UUID user2Id) {
        return dslContext.select(CONVERSATION.ID)
                .from(CONVERSATION)
                .join(CONVERSATION_MEMBER).on(CONVERSATION_MEMBER.CONVERSATION_ID.eq(CONVERSATION.ID))
                .where(CONVERSATION.IS_GROUP.eq(Boolean.FALSE))
                .and(CONVERSATION_MEMBER.USER_ID.in(List.of(user1Id, user2Id)))
                .groupBy(CONVERSATION.ID)
                .having(DSL.countDistinct(CONVERSATION_MEMBER.USER_ID).eq(2))
                .fetchOptional(CONVERSATION.ID);
    }

    public List<ConversationResponse> findConversationForUser(UUID userId, Pagination pagination) {
        var myCm = CONVERSATION_MEMBER.as("my_cm");
        var otherCm = CONVERSATION_MEMBER.as("other_cm");
        var otherUser = APP_USER.as("other_user");

        var nameField = when(CONVERSATION.IS_GROUP.isTrue(), CONVERSATION.NAME)
                .otherwise(concat(otherUser.FIRST_NAME, inline(" "), otherUser.LAST_NAME))
                .as("name");

        return dslContext.select(
                        CONVERSATION.ID,
                        CONVERSATION.IS_GROUP,
                        nameField,
                        CONVERSATION.CREATED_AT,
                        CONVERSATION.UPDATED_AT
                )
                .from(myCm)
                .join(CONVERSATION).on(myCm.CONVERSATION_ID.eq(CONVERSATION.ID))
                .leftJoin(otherCm).on(
                        CONVERSATION.ID.eq(otherCm.CONVERSATION_ID)
                                .and(otherCm.USER_ID.notEqual(userId))
                                .and(CONVERSATION.IS_GROUP.isFalse())
                )
                .leftJoin(otherUser).on(otherCm.USER_ID.eq(otherUser.ID))
                .where(myCm.USER_ID.eq(userId))
                .orderBy(CONVERSATION.UPDATED_AT.desc())
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetchInto(ConversationResponse.class);
    }
}
