package com.app.prod.polls.repository;

import com.app.prod.polls.dto.PollOptionResponse;
import com.app.prod.polls.dto.PollResponse;
import com.app.prod.polls.enums.PollTarget;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.PollFilter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.jooq.sources.tables.Poll;
import org.jooq.sources.tables.records.PollRecord;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.*;
import static org.jooq.sources.Tables.*;

@Repository
public class PollRepository extends BaseJooqRepository<Poll, PollRecord, UUID> {
    protected PollRepository(DSLContext dsl) {
        super(dsl, Poll.POLL, Poll.POLL.ID);
    }

    public List<PollResponse> getPolls(UUID userId, Pagination pagination, PollFilter pollFilter) {
        var subQuery1 = dslContext.select(POLL.ID)
                .from(POLL)
                .join(BUILDING).on(
                        BUILDING.ID.eq(POLL.BUILDING_ID).or(POLL.AREA_ID.eq(BUILDING.AREA_ID))
                )
                .join(APP_USER).on(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                .where(APP_USER.ID.eq(userId));

        //managers include
        var subQuery2 = dslContext.select(POLL.ID)
                .from(POLL)
                .where(POLL.CREATED_BY.eq(userId));

        var subQuery = subQuery1.union(subQuery2);

        Field<Boolean> userVotedField = field(
                DSL.exists(
                        selectOne()
                                .from(POLL_VOTE)
                                .where(POLL_VOTE.POLL_ID.eq(POLL.ID))
                                .and(POLL_VOTE.USER_ID.eq(userId))
                )
        ).as("user_voted");

        return dslContext.select(
            POLL.ID,
            POLL.TITLE,
            POLL.DESCRIPTION,
            APP_USER.FIRST_NAME,
            APP_USER.LAST_NAME,
            USER_ROLE.USER_ROLE_,
            POLL.ANONYMOUS,
            POLL.BUILDING_ID,
            POLL.AREA_ID,
            POLL.START_TIME,
            POLL.END_TIME,
            POLL.FINISHED,
            userVotedField,
            multiset(
                select(POLL_OPTION.ID, POLL_OPTION.OPTION_TEXT)
                      .from(POLL_OPTION)
                      .where(POLL_OPTION.POLL_ID.eq(POLL.ID))
                ).as("options")
                        .convertFrom(r -> r.map(rec ->
                                new PollOptionResponse(
                                        rec.get(POLL_OPTION.ID),
                                        rec.get(POLL_OPTION.OPTION_TEXT)
                                )
                        )))
                .from(POLL)
                .join(APP_USER).on(APP_USER.ID.eq(POLL.CREATED_BY))
                .join(USER_ROLE).on(APP_USER.USER_ROLE.eq(USER_ROLE.ID))
                .where(POLL.ID.in(subQuery))
                .and(pollFilter.parseFilterAnd())
                .orderBy(POLL.END_TIME)
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetch(record -> {

                            String firstName = record.get(APP_USER.FIRST_NAME);
                            String lastName = record.get(APP_USER.LAST_NAME);
                            String fullName = firstName + " " + lastName;

                            var areaId = record.get(POLL.AREA_ID);
                            PollTarget target = (areaId != null) ? PollTarget.AREA : PollTarget.BUILDING;
                            Boolean userVoted = record.get(userVotedField);

                            return new PollResponse(
                                    record.get(POLL.ID),
                                    record.get(POLL.TITLE),
                                    record.get(POLL.DESCRIPTION),
                                    fullName,
                                    record.get(USER_ROLE.USER_ROLE_),
                                    record.get(POLL.ANONYMOUS),
                                    target,
                                    record.get(POLL.START_TIME),
                                    record.get(POLL.END_TIME),
                                    record.get(POLL.FINISHED),
                                    userVoted,
                                    record.get("options", List.class)
                            );
                        }
                );

    }

    public List<PollRecord> getUnfinishedPollsAndFinishThem(LocalDateTime now){
        return dslContext.update(POLL)
                .set(POLL.FINISHED, true)
                .where(POLL.FINISHED.eq(false))
                .and(POLL.END_TIME.lt(now))
                .returning()
                .fetch();
    }

    public int getNumberOfPeopleEligibleToVoteBuildingStrategy(UUID pollId) {
        return Optional.ofNullable(
                dslContext.selectCount()
                        .from(POLL)
                        .join(BUILDING).on(BUILDING.ID.eq(POLL.BUILDING_ID))
                        .join(APP_USER).on(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                        .where(POLL.ID.eq(pollId))
                        .fetchOneInto(Integer.class)
        ).orElse(0);
    }

    public int getNumberOfPeopleEligibleToVoteAreaStrategy(UUID pollId) {
        return Optional.ofNullable(
                dslContext.selectCount()
                        .from(POLL)
                        .join(AREA).on(AREA.ID.eq(POLL.AREA_ID))
                        .join(BUILDING).on(BUILDING.AREA_ID.eq(AREA.ID))
                        .join(APP_USER).on(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                        .where(POLL.ID.eq(pollId))
                        .fetchOneInto(Integer.class)
        ).orElse(0);
    }
}
