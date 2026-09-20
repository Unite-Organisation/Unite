package com.app.prod.event.repository;

import com.app.prod.event.device.DeviceSignals;
import com.app.prod.utils.BaseJooqRepository;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.sources.tables.EventMemberMetadata;
import org.jooq.sources.tables.records.EventMemberMetadataRecord;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.jooq.sources.Tables.EVENT_MEMBER;
import static org.jooq.sources.Tables.EVENT_MEMBER_METADATA;

@Repository
public class EventMemberMetadataRepository extends BaseJooqRepository<EventMemberMetadata, EventMemberMetadataRecord, UUID> {

    protected EventMemberMetadataRepository(DSLContext dsl) {
        super(dsl, EVENT_MEMBER_METADATA, EVENT_MEMBER_METADATA.ID);
    }

    public void save(UUID memberId, DeviceSignals signals, LocalDateTime now) {
        dslContext.insertInto(EVENT_MEMBER_METADATA)
                .set(EVENT_MEMBER_METADATA.ID, UUID.randomUUID())
                .set(EVENT_MEMBER_METADATA.MEMBER_ID, memberId)
                .set(EVENT_MEMBER_METADATA.DEVICE_KEY, signals.deviceKey())
                .set(EVENT_MEMBER_METADATA.DEVICE_ID_HASH, signals.deviceIdHash())
                .set(EVENT_MEMBER_METADATA.IP_HASH, signals.ipHash())
                .set(EVENT_MEMBER_METADATA.IP_NETWORK_HASH, signals.ipNetworkHash())
                .set(EVENT_MEMBER_METADATA.USER_AGENT_HASH, signals.userAgentHash())
                .set(EVENT_MEMBER_METADATA.RENDERER_HASH, signals.rendererHash())
                .set(EVENT_MEMBER_METADATA.CANVAS_HASH, signals.canvasHash())
                .set(EVENT_MEMBER_METADATA.OS_FAMILY, signals.osFamily())
                .set(EVENT_MEMBER_METADATA.OS_VERSION, signals.osVersion())
                .set(EVENT_MEMBER_METADATA.BROWSER_FAMILY, signals.browserFamily())
                .set(EVENT_MEMBER_METADATA.BROWSER_MAJOR, signals.browserMajor())
                .set(EVENT_MEMBER_METADATA.DEVICE_MODEL, signals.deviceModel())
                .set(EVENT_MEMBER_METADATA.SCREEN, signals.screen())
                .set(EVENT_MEMBER_METADATA.PIXEL_RATIO, decimal(signals.pixelRatio()))
                .set(EVENT_MEMBER_METADATA.COLOR_DEPTH, signals.colorDepth())
                .set(EVENT_MEMBER_METADATA.HARDWARE_CONCURRENCY, signals.hardwareConcurrency())
                .set(EVENT_MEMBER_METADATA.DEVICE_MEMORY, decimal(signals.deviceMemory()))
                .set(EVENT_MEMBER_METADATA.MAX_TOUCH_POINTS, signals.maxTouchPoints())
                .set(EVENT_MEMBER_METADATA.TIME_ZONE, signals.timeZone())
                .set(EVENT_MEMBER_METADATA.TIME_ZONE_OFFSET, signals.timeZoneOffset())
                .set(EVENT_MEMBER_METADATA.LANGUAGES, signals.languages())
                .set(EVENT_MEMBER_METADATA.CREATED_AT, now)
                .set(EVENT_MEMBER_METADATA.LAST_SEEN_AT, now)
                .onConflict(EVENT_MEMBER_METADATA.MEMBER_ID, EVENT_MEMBER_METADATA.DEVICE_KEY)
                .doUpdate()
                // the network moves, the hardware does not
                .set(EVENT_MEMBER_METADATA.IP_HASH, signals.ipHash())
                .set(EVENT_MEMBER_METADATA.IP_NETWORK_HASH, signals.ipNetworkHash())
                .set(EVENT_MEMBER_METADATA.TIME_ZONE, signals.timeZone())
                .set(EVENT_MEMBER_METADATA.TIME_ZONE_OFFSET, signals.timeZoneOffset())
                .set(EVENT_MEMBER_METADATA.LAST_SEEN_AT, now)
                .execute();
    }

    public Map<UUID, List<DeviceSignals>> findByEvent(UUID eventId) {
        return dslContext.select(EVENT_MEMBER_METADATA.fields())
                .from(EVENT_MEMBER_METADATA)
                .join(EVENT_MEMBER).on(EVENT_MEMBER.ID.eq(EVENT_MEMBER_METADATA.MEMBER_ID))
                .where(EVENT_MEMBER.EVENT_ID.eq(eventId))
                .fetchGroups(
                        record -> record.get(EVENT_MEMBER_METADATA.MEMBER_ID),
                        EventMemberMetadataRepository::toSignals
                );
    }

    private static DeviceSignals toSignals(Record record) {
        return new DeviceSignals(
                record.get(EVENT_MEMBER_METADATA.DEVICE_ID_HASH),
                record.get(EVENT_MEMBER_METADATA.IP_HASH),
                record.get(EVENT_MEMBER_METADATA.IP_NETWORK_HASH),
                record.get(EVENT_MEMBER_METADATA.USER_AGENT_HASH),
                record.get(EVENT_MEMBER_METADATA.RENDERER_HASH),
                record.get(EVENT_MEMBER_METADATA.CANVAS_HASH),
                record.get(EVENT_MEMBER_METADATA.OS_FAMILY),
                record.get(EVENT_MEMBER_METADATA.OS_VERSION),
                record.get(EVENT_MEMBER_METADATA.BROWSER_FAMILY),
                record.get(EVENT_MEMBER_METADATA.BROWSER_MAJOR),
                record.get(EVENT_MEMBER_METADATA.DEVICE_MODEL),
                record.get(EVENT_MEMBER_METADATA.SCREEN),
                plain(record.get(EVENT_MEMBER_METADATA.PIXEL_RATIO)),
                record.get(EVENT_MEMBER_METADATA.COLOR_DEPTH),
                record.get(EVENT_MEMBER_METADATA.HARDWARE_CONCURRENCY),
                plain(record.get(EVENT_MEMBER_METADATA.DEVICE_MEMORY)),
                record.get(EVENT_MEMBER_METADATA.MAX_TOUCH_POINTS),
                record.get(EVENT_MEMBER_METADATA.TIME_ZONE),
                record.get(EVENT_MEMBER_METADATA.TIME_ZONE_OFFSET),
                record.get(EVENT_MEMBER_METADATA.LANGUAGES)
        );
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static String plain(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
