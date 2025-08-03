package com.app.prod.utils;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;

import java.util.List;

public abstract class BaseJooqRepository<T extends Table<R>, R extends Record> {

    protected DSLContext dslContext;
    protected final T table;

    protected BaseJooqRepository(DSLContext dsl, T table) {
        this.dslContext = dsl;
        this.table = table;
    }

    public List<R> findAll(){
        return dslContext.selectFrom(table).fetch();
    }

    public int insert(R record){
        return dslContext.insertInto(table).set(record).execute();
    }
}
