package com.app.prod.utils;

import org.jooq.*;

import java.util.List;

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

}
