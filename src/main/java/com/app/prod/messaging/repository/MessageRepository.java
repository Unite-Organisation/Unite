package com.app.prod.messaging.repository;

import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.Pagination;
import org.jooq.DSLContext;
import org.jooq.TableField;
import org.jooq.sources.tables.Message;
import org.jooq.sources.tables.records.MessageRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class MessageRepository extends BaseJooqRepository<Message, MessageRecord, UUID> {
    protected MessageRepository(DSLContext dsl) {
        super(dsl, Message.MESSAGE, Message.MESSAGE.ID);
    }

    public List<MessageRecord> findByConversationId(UUID conversationid, Pagination pagination) {
        return dslContext.selectFrom(table)
                .where(table.CONVERSATION_ID.eq(conversationid))
                .orderBy(table.SEND_AT)
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetch();

    }
}
