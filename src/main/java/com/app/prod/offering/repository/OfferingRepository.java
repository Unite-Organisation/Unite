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

    public List<OfferingResponse> getOfferings(OfferingFilter filter) {
        return dslContext.select(
                OFFERING.ID,
                OFFERING.TITLE,
                OFFERING.DESCRIPTION,
                OFFERING.CATEGORY,
                OFFERING.IS_ACTIVE,
                OFFERING.PRICE,
                OFFERING.END_DATE,
                OFFERING.CREATED_AT,
                USERS.ID,
                USERS.FIRST_NAME,
                USERS.LAST_NAME,
                USER_ROLES.USER_ROLE
        )
                .from(OFFERING)
                .leftJoin(USERS).on(OFFERING.USER_PROVIDER.eq(USERS.ID))
                .leftJoin(USER_ROLES).on(USERS.USER_ROLE.eq(USER_ROLES.ID))
                .where(filter.parseFilterAnd())
                .and(OFFERING.IS_ACTIVE.eq(Boolean.TRUE))
                .fetch(record -> new OfferingResponse(
                        record.get(OFFERING.ID),
                        record.get(OFFERING.TITLE),
                        record.get(OFFERING.DESCRIPTION),
                        record.get(OFFERING.CATEGORY),
                        record.get(OFFERING.IS_ACTIVE),
                        record.get(OFFERING.PRICE),
                        record.get(OFFERING.END_DATE),
                        record.get(OFFERING.CREATED_AT),
                        new BasicUserData(
                                record.get(USERS.ID),
                                record.get(USERS.FIRST_NAME),
                                record.get(USERS.LAST_NAME),
                                record.get(USER_ROLES.USER_ROLE)
                        )
                ));

    }

    public boolean cancelOffering(UUID id){
        return dslContext.update(OFFERING)
                .set(OFFERING.IS_ACTIVE, Boolean.FALSE)
                .execute() > 0;
    }
}
