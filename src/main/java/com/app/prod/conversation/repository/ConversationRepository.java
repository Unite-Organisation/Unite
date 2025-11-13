package com.app.prod.conversation.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.sources.tables.Conversation;
import org.jooq.sources.tables.records.ConversationRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.CONVERSATION;
import static org.jooq.sources.Tables.CONVERSATION_MEMBER;

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
}
