package com.app.prod.offering.repository;

import com.app.prod.offering.dto.OfferingResponse;
import com.app.prod.user.dto.BasicUserData;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.filters.OfferingFilter;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Offering;
import org.jooq.sources.tables.records.OfferingRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Repository
public class OfferingRepository extends BaseJooqRepository<Offering, OfferingRecord, UUID> {
    protected OfferingRepository(DSLContext dsl) {
        super(dsl, OFFERING, OFFERING.ID);
    }

    public List<OfferingResponse> getOfferings(OfferingFilter filter, UUID userId) {
        return dslContext.select(
                OFFERING.ID,
                OFFERING.TITLE,
                OFFERING.DESCRIPTION,
                OFFERING.CATEGORY,
                OFFERING.IS_ACTIVE,
                OFFERING.PRICE,
                OFFERING.END_DATE,
                OFFERING.CREATED_AT,
                OFFERING.USER_PROVIDER,
                APP_USER.ID,
                APP_USER.FIRST_NAME,
                APP_USER.LAST_NAME,
                USER_ROLE.USER_ROLE_
        )
                .from(OFFERING)
                .leftJoin(APP_USER).on(OFFERING.USER_PROVIDER.eq(APP_USER.ID))
                .leftJoin(USER_ROLE).on(APP_USER.USER_ROLE.eq(USER_ROLE.ID))
                .where(filter.parseFilterAnd())
                .and(OFFERING.IS_ACTIVE.eq(Boolean.TRUE))
                .fetch(record -> {
                    boolean createByUser = record.get(OFFERING.USER_PROVIDER).equals(userId);

                    return new OfferingResponse(
                        record.get(OFFERING.ID),
                        record.get(OFFERING.TITLE),
                        record.get(OFFERING.DESCRIPTION),
                        record.get(OFFERING.CATEGORY),
                        record.get(OFFERING.IS_ACTIVE),
                        record.get(OFFERING.PRICE),
                        record.get(OFFERING.END_DATE),
                        record.get(OFFERING.CREATED_AT),
                        createByUser,
                        new BasicUserData(
                                record.get(APP_USER.ID),
                                record.get(APP_USER.FIRST_NAME),
                                record.get(APP_USER.LAST_NAME),
                                record.get(USER_ROLE.USER_ROLE_)
                        ));

                });

    }

    public boolean cancelOffering(UUID id){
        return dslContext.update(OFFERING)
                .set(OFFERING.IS_ACTIVE, Boolean.FALSE)
                .execute() > 0;
    }
}
