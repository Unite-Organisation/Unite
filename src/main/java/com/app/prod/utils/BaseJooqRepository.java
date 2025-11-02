package com.app.prod.utils;

import com.app.prod.utils.filters.PredicateFilter;
import org.jooq.*;

import java.util.List;
import java.util.Optional;

/* table, record, keyType */
public abstract class BaseJooqRepository<T extends Table<R>, R extends TableRecord<R>, K> {

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

    public List<R> findFilteredOr(PredicateFilter filter){
        return dslContext.selectFrom(table)
                .where(filter.parseFilterOr())
                .fetch();
    }

    public List<R> findFilteredAnd(PredicateFilter filter){
        return dslContext.selectFrom(table)
                .where(filter.parseFilterAnd())
                .fetch();
    }

}
