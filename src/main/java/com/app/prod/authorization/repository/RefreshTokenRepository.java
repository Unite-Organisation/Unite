package com.app.prod.authorization.repository;

import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.sources.tables.RefreshToken;
import org.jooq.sources.tables.records.RefreshTokenRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.REFRESH_TOKEN;

@Repository
public class RefreshTokenRepository extends BaseJooqRepository<RefreshToken, RefreshTokenRecord, UUID> {
    protected RefreshTokenRepository(DSLContext dsl) {
        super(dsl, REFRESH_TOKEN, REFRESH_TOKEN.ID);
    }

    public List<RefreshTokenRecord> findByToken(String oldToken) {
        return dslContext.selectFrom(REFRESH_TOKEN)
                .where(REFRESH_TOKEN.TOKEN.eq(oldToken))
                .fetch();
    }

    public void revokeAllUserTokens(UUID userId, LocalDateTime now) {
        dslContext.update(REFRESH_TOKEN)
                .set(REFRESH_TOKEN.REVOKED_AT, now)
                .where(REFRESH_TOKEN.USER_ID.eq(userId))
                .and(REFRESH_TOKEN.REVOKED_AT.isNull())
                .and(REFRESH_TOKEN.EXPIRY_DATE.gt(now))
                .execute();
    }

}
