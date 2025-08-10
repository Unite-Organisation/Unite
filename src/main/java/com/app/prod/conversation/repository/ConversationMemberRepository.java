package com.app.prod.conversation.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.TableField;
import org.jooq.sources.tables.ConversationMember;
import org.jooq.sources.tables.records.ConversationMemberRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ConversationMemberRepository extends BaseJooqRepository<ConversationMember, ConversationMemberRecord, UUID> {
    protected ConversationMemberRepository(DSLContext dsl) {
        super(dsl, ConversationMember.CONVERSATION_MEMBER, ConversationMember.CONVERSATION_MEMBER.CONVERSATION_ID);
    }

    public boolean userBelongToConversation(UUID userID, UUID conversationId){
        return dslContext.fetchExists(
                dslContext.selectOne()
                        .from(table)
                        .where(table.USER_ID.eq(userID)
                                .and(table.CONVERSATION_ID.eq(conversationId)))
        );
    }
}
