package com.app.prod.cleaning;

import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Cleans one table of rows that outlived their use. A subclass is a {@code @Component} in the package of
 * the feature owning the table; {@link DatabaseCleaningService} runs every one of them.
 *
 * <p>A subclass says which rows are expired ({@link #findExpiredEntityIds(LocalDateTime)}) and how long
 * they are kept ({@link #getRetentionPeriod()}). It overrides {@link #deleteRecordsNotLinkedByForeignKey(List)}
 * only when something points at the table without a foreign key.
 */
public abstract class DatabaseEntityCleaningPolicy {

    protected final DSLContext dslContext;
    private final Table<?> entityTable;
    private final TableField<?, UUID> entityIdField;

    protected DatabaseEntityCleaningPolicy(DSLContext dslContext, Table<?> entityTable, TableField<?, UUID> entityIdField) {
        this.dslContext = dslContext;
        this.entityTable = entityTable;
        this.entityIdField = entityIdField;
    }

    public abstract String getEntityName();
    public abstract Duration getRetentionPeriod();
    protected abstract List<UUID> findExpiredEntityIds(LocalDateTime expirationCutoff);

    protected void deleteEntitiesWithDatabaseCascade(List<UUID> entityIds) {
        dslContext.deleteFrom(entityTable)
                .where(entityIdField.in(entityIds))
                .execute();
    }

    protected abstract void deleteRecordsNotLinkedByForeignKey(List<UUID> deletedEntityIds);

    public final int cleanExpiredEntities(LocalDateTime now, boolean dryRun) {
        List<UUID> expiredEntityIds = findExpiredEntityIds(now.minus(getRetentionPeriod()));
        if (dryRun || expiredEntityIds.isEmpty()) {
            return expiredEntityIds.size();
        }

        deleteEntitiesWithDatabaseCascade(expiredEntityIds);
        deleteRecordsNotLinkedByForeignKey(expiredEntityIds);
        return expiredEntityIds.size();
    }
}
