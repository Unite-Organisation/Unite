package com.app.prod.utils;

import com.app.prod.utils.filters.PredicateFilter;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.sources.Tables.*;
import static org.jooq.sources.Tables.BUILDING;
import static org.jooq.sources.Tables.BUILDING_MANAGER;

/* table, record, keyType */
public abstract class BaseJooqRepository<T extends Table<R>, R extends UpdatableRecord<R>, K> {

    protected DSLContext dslContext;
    protected final T table;
    protected final TableField<R, K> id;

    protected BaseJooqRepository(DSLContext dsl, T table, TableField<R, K> id) {
        this.dslContext = dsl;
        this.table = table;
        this.id = id;
    }

    public List<R> findAll(){
        return dslContext.selectFrom(table).fetch();
    }

    public int insertOne(R record){
        return dslContext.insertInto(table).set(record).execute();
    }

    public void insertMany(List<R> records){
        dslContext.batchInsert(records).execute();
    }

    public void update(R record) {
        record.store();
    }

    public boolean exists(K recordId){
        return dslContext.selectOne()
                .from(table)
                .where(id.eq(recordId))
                .fetchOptional()
                .isPresent();
    }

    public Optional<R> findById(K recordId){
        return dslContext.selectFrom(table)
                .where(id.eq(recordId))
                .fetchOptional();
    }

    public List<R> findFilteredAnd(PredicateFilter filter){
        return dslContext.selectFrom(table)
                .where(filter.parseFilter())
                .fetch();
    }

    protected Table<Record2<UUID, UUID>> buildingsVisibleTo(UUID userId) {
        return DSL.select(BUILDING.ID, BUILDING.AREA_ID)
                .from(BUILDING)
                .join(APP_USER).on(APP_USER.BUILDING_ID.eq(BUILDING.ID))
                .where(APP_USER.ID.eq(userId))
                .union(
                        DSL.select(BUILDING.ID, BUILDING.AREA_ID)
                                .from(BUILDING)
                                .join(BUILDING_MANAGER).on(BUILDING_MANAGER.BUILDING_ID.eq(BUILDING.ID))
                                .where(BUILDING_MANAGER.USER_ID.eq(userId))
                )
                .asTable("visible_building");
    }

}
