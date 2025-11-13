package com.app.prod.conversation.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Conversation;
import org.jooq.sources.tables.records.ConversationRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ConversationRepository extends BaseJooqRepository<Conversation, ConversationRecord, UUID> {
    protected ConversationRepository(DSLContext dsl) {
        super(dsl, Conversation.CONVERSATION, Conversation.CONVERSATION.ID);
    }
}
