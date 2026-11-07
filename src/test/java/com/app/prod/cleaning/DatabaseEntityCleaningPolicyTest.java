package com.app.prod.cleaning;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DatabaseEntityCleaningPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 3, 0);
    private static final Duration RETENTION_PERIOD = Duration.ofDays(30);
    private static final List<UUID> EXPIRED_ENTITY_IDS = List.of(UUID.randomUUID(), UUID.randomUUID());

    @Test
    void shouldFindExpiredThenDeleteWithCascadeThenDeleteUnlinkedRecords() {
        DatabaseEntityCleaningPolicy policy = policyFindingExpiredEntities();

        int cleanedEntityCount = policy.cleanExpiredEntities(NOW, false);

        assertThat(cleanedEntityCount).isEqualTo(2);
        InOrder pipelineOrder = inOrder(policy);
        pipelineOrder.verify(policy).findExpiredEntityIds(NOW.minus(RETENTION_PERIOD));
        pipelineOrder.verify(policy).deleteEntitiesWithDatabaseCascade(EXPIRED_ENTITY_IDS);
        pipelineOrder.verify(policy).deleteRecordsNotLinkedByForeignKey(EXPIRED_ENTITY_IDS);
    }

    @Test
    void shouldOnlyCountExpiredEntitiesOnADryRun() {
        DatabaseEntityCleaningPolicy policy = policyFindingExpiredEntities();

        int cleanedEntityCount = policy.cleanExpiredEntities(NOW, true);

        assertThat(cleanedEntityCount).isEqualTo(2);
        verify(policy, never()).deleteEntitiesWithDatabaseCascade(any());
        verify(policy, never()).deleteRecordsNotLinkedByForeignKey(any());
    }

    private static DatabaseEntityCleaningPolicy policyFindingExpiredEntities() {
        DatabaseEntityCleaningPolicy policy = mock(DatabaseEntityCleaningPolicy.class, CALLS_REAL_METHODS);
        doReturn(RETENTION_PERIOD).when(policy).getRetentionPeriod();
        doReturn(EXPIRED_ENTITY_IDS).when(policy).findExpiredEntityIds(NOW.minus(RETENTION_PERIOD));
        doNothing().when(policy).deleteEntitiesWithDatabaseCascade(any());
        doNothing().when(policy).deleteRecordsNotLinkedByForeignKey(any());
        return policy;
    }
}
