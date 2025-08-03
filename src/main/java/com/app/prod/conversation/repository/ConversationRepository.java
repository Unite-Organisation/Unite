package com.app.prod.conversation.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Conversations;
import org.jooq.sources.tables.records.ConversationsRecord;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ConversationRepository extends BaseJooqRepository<Conversations, ConversationsRecord, UUID> {
    protected ConversationRepository(DSLContext dsl) {
        super(dsl, Conversations.CONVERSATIONS, Conversations.CONVERSATIONS.ID);
    }
}
