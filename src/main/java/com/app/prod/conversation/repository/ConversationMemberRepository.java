package com.app.prod.conversation.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.ConversationMember;
import org.jooq.sources.tables.records.ConversationMemberRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.jooq.impl.DSL.count;
import static org.jooq.sources.Tables.CONVERSATION;
import static org.jooq.sources.Tables.CONVERSATION_MEMBER;

@Repository
public class ConversationMemberRepository extends BaseJooqRepository<ConversationMember, ConversationMemberRecord, UUID> {
    protected ConversationMemberRepository(DSLContext dsl) {
        super(dsl, ConversationMember.CONVERSATION_MEMBER, ConversationMember.CONVERSATION_MEMBER.CONVERSATION_ID);
    }

    public boolean userBelongToConversation(UUID userID, UUID conversationId){
        return dslContext.fetchExists(
                dslContext.selectOne()
                        .from(CONVERSATION_MEMBER)
                        .where(CONVERSATION_MEMBER.USER_ID.eq(userID)
                                .and(CONVERSATION_MEMBER.CONVERSATION_ID.eq(conversationId)))
        );
    }

    public boolean existsPrivateConversationBetweenContacts(UUID user1, UUID user2){
        return dslContext.fetchExists(
                dslContext.select(CONVERSATION.ID)
                        .from(CONVERSATION)
                        .join(CONVERSATION_MEMBER)
                        .on(CONVERSATION.ID.eq(CONVERSATION_MEMBER.CONVERSATION_ID))
                        .where(CONVERSATION.IS_GROUP.eq(false))
                        .groupBy(CONVERSATION.ID)
                        .having(count().eq(2))
                        .and(count().filterWhere(CONVERSATION_MEMBER.USER_ID.eq(user1)).eq(1))
                        .and(count().filterWhere(CONVERSATION_MEMBER.USER_ID.eq(user2)).eq(1))
        );
    }
}
