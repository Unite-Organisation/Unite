package com.app.prod.cleaning;

import com.app.prod.cleaning.dtos.DatabaseCleaningResponse;
import com.app.prod.cleaning.dtos.EntityCleaningResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseCleaningServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 3, 0);

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void shouldCleanEveryEntityEvenWhenOneFails() {
        DatabaseEntityCleaningPolicy brokenPolicy = policyNamed("broken");
        when(brokenPolicy.cleanExpiredEntities(NOW, false)).thenThrow(new RuntimeException("boom"));
        DatabaseEntityCleaningPolicy healthyPolicy = policyNamed("healthy");
        when(healthyPolicy.cleanExpiredEntities(NOW, false)).thenReturn(3);

        DatabaseCleaningResponse response = new DatabaseCleaningService(List.of(brokenPolicy, healthyPolicy), transactionTemplate)
                .cleanExpiredEntities(NOW, false);

        assertThat(response.entityCleaningResults()).containsExactly(
                new EntityCleaningResult("broken", 0, true),
                new EntityCleaningResult("healthy", 3, false)
        );
    }

    private static DatabaseEntityCleaningPolicy policyNamed(String entityName) {
        DatabaseEntityCleaningPolicy policy = mock(DatabaseEntityCleaningPolicy.class);
        when(policy.getEntityName()).thenReturn(entityName);
        return policy;
    }
}
