package com.app.prod.requests.repository;

import com.app.prod.requests.dto.RequestResponse;
import com.app.prod.requests.enums.RequestStatus;
import com.app.prod.user.dto.UserData;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.filters.RequestFilter;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Request;
import org.jooq.sources.tables.records.RequestRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Repository
public class RequestRepository extends BaseJooqRepository<Request, RequestRecord, UUID> {
    protected RequestRepository(DSLContext dsl) {
        super(dsl, REQUEST, REQUEST.ID);
    }

    public void updateStatus(RequestStatus status, UUID id){
        dslContext.update(REQUEST)
                .set(REQUEST.STATUS, status.name())
                .where(REQUEST.ID.eq(id))
                .execute();
    }

    public List<RequestResponse> findAllForArea(RequestFilter filter) {
        var uNeed = APP_USER.as("u_need");
        var uDonor = APP_USER.as("u_donor");

        return dslContext
                .select(
                        REQUEST.ID,
                        REQUEST.TITLE,
                        REQUEST.DESCRIPTION,
                        REQUEST.STATUS,
                        REQUEST.CREATED_AT,
                        REQUEST_DONOR.DEADLINE_AT,

                        uNeed.ID.as("need_id"),
                        uNeed.FIRST_NAME.as("need_first_name"),
                        uNeed.LAST_NAME.as("need_last_name"),

                        uDonor.ID.as("donor_id"),
                        uDonor.FIRST_NAME.as("donor_first_name"),
                        uDonor.LAST_NAME.as("donor_last_name")
                )
                .from(REQUEST)
                .join(uNeed).on(uNeed.ID.eq(REQUEST.USER_IN_NEED))
                .leftJoin(REQUEST_DONOR).on(REQUEST_DONOR.REQUEST_ID.eq(REQUEST.ID))
                .leftJoin(uDonor).on(uDonor.ID.eq(REQUEST_DONOR.DONOR_ID))
                .where(filter.parseFilter())
                .fetch()
                .map(record -> new RequestResponse(
                        record.get(REQUEST.ID),
                        record.get(REQUEST.TITLE),
                        record.get(REQUEST.DESCRIPTION),
                        new UserData(
                                record.get("need_id", UUID.class),
                                record.get("need_first_name", String.class),
                                record.get("need_last_name", String.class)
                        ),
                        record.get("donor_id") != null ? new UserData(
                                record.get("donor_id", UUID.class),
                                record.get("donor_first_name", String.class),
                                record.get("donor_last_name", String.class)
                        ) : null,
                        RequestStatus.valueOf(record.get(REQUEST.STATUS)),
                        record.get(REQUEST.CREATED_AT),
                        record.get(REQUEST_DONOR.DEADLINE_AT) != null
                                ? record.get(REQUEST_DONOR.DEADLINE_AT)
                                : null
                ));

    }

}
