package com.app.prod.utils;

import org.jooq.Record;
import org.jooq.RecordMapper;

import java.util.Collection;
import java.util.List;

// not used yet
public abstract class JooqMapper<R extends Record, E> implements RecordMapper<R, E> {
    public abstract E _map(R record);

    public Collection<E> map(Collection<R> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        return records.stream()
                .map(this::_map)
                .toList();
    }

    public E map(R record) {
        if (record == null) {
            return null;
        }

        return _map(record);
    }

}
