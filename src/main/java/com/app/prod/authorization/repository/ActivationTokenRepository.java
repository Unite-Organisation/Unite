package com.app.prod.authorization.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.ActivationToken;
import org.jooq.sources.tables.records.ActivationTokenRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.tables.ActivationToken.ACTIVATION_TOKEN;

@Repository
public class ActivationTokenRepository extends BaseJooqRepository<ActivationToken, ActivationTokenRecord, UUID> {

    protected ActivationTokenRepository(DSLContext dsl) {
        super(dsl, ACTIVATION_TOKEN, ACTIVATION_TOKEN.ID);
    }

    public Optional<ActivationTokenRecord> findByTokenHash(String tokenHash) {
        return dslContext.selectFrom(ACTIVATION_TOKEN)
                .where(ACTIVATION_TOKEN.TOKEN_HASH.eq(tokenHash))
                .fetchOptional();
    }

    /**
     * Returns the number of rows burned - zero means someone got there first with the same token.
     */
    public int markUsed(UUID tokenId, LocalDateTime usedAt) {
        return dslContext.update(ACTIVATION_TOKEN)
                .set(ACTIVATION_TOKEN.USED_AT, usedAt)
                .where(ACTIVATION_TOKEN.ID.eq(tokenId))
                .and(ACTIVATION_TOKEN.USED_AT.isNull())
                .execute();
    }
}
