package com.app.prod.polls.repository;

import com.app.prod.polls.dto.PollResponse;
import com.app.prod.polls.enums.PollTarget;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.Pagination;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.sources.tables.Polls;
import org.jooq.sources.tables.records.PollsRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;
import static org.jooq.sources.Tables.*;

@Repository
public class PollRepository extends BaseJooqRepository<Polls, PollsRecord, UUID> {
    protected PollRepository(DSLContext dsl) {
        super(dsl, Polls.POLLS, Polls.POLLS.ID);
    }

    public List<PollResponse> getPolls(UUID userId, Pagination pagination) {
        var subQuery = dslContext.select(POLLS.ID)
                .from(POLLS)
                .join(BUILDINGS).on(
                        BUILDINGS.ID.eq(POLLS.BUILDING_ID).or(POLLS.AREA_ID.eq(BUILDINGS.AREA_ID))
                )
                .join(USERS).on(USERS.BUILDING_ID.eq(BUILDINGS.ID))
                .where(USERS.ID.eq(userId));

        return dslContext.select(
            POLLS.ID,
            POLLS.TITLE,
            POLLS.DESCRIPTION,
            USERS.FIRST_NAME,
            USERS.LAST_NAME,
            USER_ROLES.USER_ROLE,
            POLLS.ANONYMOUS,
            POLLS.BUILDING_ID,
            POLLS.AREA_ID,
            POLLS.START_TIME,
            POLLS.END_TIME,
            multiset(
                    select(POLL_OPTIONS.OPTION_TEXT)
                            .from(POLL_OPTIONS)
                            .where(POLL_OPTIONS.POLL_ID.eq(POLLS.ID))
                ).as("options").convertFrom(r -> r.map(Record1::value1))
            )
                .from(POLLS)
                .join(USERS).on(USERS.ID.eq(POLLS.CREATED_BY))
                .join(USER_ROLES).on(USERS.USER_ROLE.eq(USER_ROLES.ID))
                .where(POLLS.ID.in(subQuery))
                .orderBy(POLLS.END_TIME)
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetch(record -> {

                            String firstName = record.get(USERS.FIRST_NAME);
                            String lastName = record.get(USERS.LAST_NAME);
                            String fullName = firstName + " " + lastName;

                            var areaId = record.get(POLLS.AREA_ID);
                            PollTarget target = (areaId != null) ? PollTarget.AREA : PollTarget.BUILDING;

                            return new PollResponse(
                                    record.get(POLLS.ID),
                                    record.get(POLLS.TITLE),
                                    record.get(POLLS.DESCRIPTION),
                                    fullName,
                                    record.get(USER_ROLES.USER_ROLE),
                                    record.get(POLLS.ANONYMOUS),
                                    target,
                                    record.get(POLLS.START_TIME),
                                    record.get(POLLS.END_TIME),
                                    record.get("options", List.class)
                            );
                        }
                );

    }
}
