package com.app.prod.utils.json;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.JSONB;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
@RequiredArgsConstructor
public class JsonbService {

    private static final JSONB EMPTY_ARRAY = JSONB.valueOf("[]");
    private final ObjectMapper objectMapper;

    public JSONB toJsonb(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            log.error("Failed to serialize {} to jsonb", value.getClass().getSimpleName(), e);
            throw new IllegalApplicationStateException(AppError.of(Code.JSON_SERIALIZATION_ERROR));
        }
    }

    public <T> T fromJsonb(JSONB jsonb, Class<T> type) {
        if (jsonb == null || jsonb.data() == null || jsonb.data().isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonb.data(), type);
        } catch (Exception e) {
            log.error("Failed to deserialize jsonb to {}", type.getSimpleName(), e);
            throw new IllegalApplicationStateException(AppError.of(Code.JSON_SERIALIZATION_ERROR));
        }
    }

    public <T> List<T> listFromJsonb(JSONB jsonb, Class<T> type) {
        JSONB source = jsonb == null || jsonb.data() == null || jsonb.data().isBlank() ? EMPTY_ARRAY : jsonb;

        try {
            var listType = objectMapper.getTypeFactory().constructCollectionType(List.class, type);
            return objectMapper.readValue(source.data(), listType);
        } catch (Exception e) {
            log.error("Failed to deserialize jsonb to list of {}", type.getSimpleName(), e);
            throw new IllegalApplicationStateException(AppError.of(Code.JSON_SERIALIZATION_ERROR));
        }
    }
}
