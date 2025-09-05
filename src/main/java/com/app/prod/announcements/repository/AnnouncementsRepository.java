package com.app.prod.announcements.repository;

import com.app.prod.announcements.dto.AnnouncementResponse;
import com.app.prod.utils.BaseJooqRepository;
import com.app.prod.utils.Pagination;
import org.jooq.DSLContext;
import org.jooq.sources.tables.Announcements;
import org.jooq.sources.tables.records.AnnouncementsRecord;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static org.jooq.sources.Tables.*;

@Repository
public class AnnouncementsRepository extends BaseJooqRepository<Announcements, AnnouncementsRecord, UUID> {
    protected AnnouncementsRepository(DSLContext dsl) {
        super(dsl, Announcements.ANNOUNCEMENTS, Announcements.ANNOUNCEMENTS.ID);
    }

    public List<AnnouncementResponse> findForUser(UUID userId, Pagination pagination) {
        return dslContext.selectDistinct(
                    ANNOUNCEMENTS.ID,
                    ANNOUNCEMENTS.NAME,
                    ANNOUNCEMENTS.AREA_ID,
                    ANNOUNCEMENTS.BUILDING_ID,
                    ANNOUNCEMENTS.CREATED_BY,
                    ANNOUNCEMENTS.CONTENT,
                    ANNOUNCEMENTS.RELATED_DATE,
                    ANNOUNCEMENTS.CREATED_AT
                )
                .from(USERS)
                .join(BUILDINGS).on(BUILDINGS.ID.eq(USERS.BUILDING_ID))
                .join(AREAS).on(AREAS.ID.eq(BUILDINGS.AREA_ID))
                .join(ANNOUNCEMENTS).on(ANNOUNCEMENTS.AREA_ID.eq(AREAS.ID))
                .join(ANNOUNCEMENTS).on(
                        ANNOUNCEMENTS.BUILDING_ID.eq(BUILDINGS.ID)
                                .or(ANNOUNCEMENTS.AREA_ID.eq(AREAS.ID))
                )
                .where(USERS.ID.eq(userId))
                .orderBy(ANNOUNCEMENTS.CREATED_AT)
                .offset(pagination.getOffset())
                .limit(pagination.pageSize())
                .fetch(record -> new AnnouncementResponse(
                        record.get(ANNOUNCEMENTS.ID),
                        record.get(ANNOUNCEMENTS.NAME),
                        record.get(ANNOUNCEMENTS.AREA_ID),
                        record.get(ANNOUNCEMENTS.BUILDING_ID),
                        record.get(ANNOUNCEMENTS.CREATED_BY),
                        record.get(ANNOUNCEMENTS.CONTENT),
                        record.get(ANNOUNCEMENTS.RELATED_DATE),
                        record.get(ANNOUNCEMENTS.CREATED_AT)
                ));

    }
}
