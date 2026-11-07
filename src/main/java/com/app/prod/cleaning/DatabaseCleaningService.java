package com.app.prod.cleaning;

import com.app.prod.cleaning.dtos.DatabaseCleaningResponse;
import com.app.prod.cleaning.dtos.EntityCleaningResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatabaseCleaningService {

    private final List<DatabaseEntityCleaningPolicy> entityCleaningPolicies;
    private final TransactionTemplate transactionTemplate;

    public DatabaseCleaningResponse cleanExpiredEntities(LocalDateTime now, boolean dryRun) {
        List<EntityCleaningResult> entityCleaningResults = entityCleaningPolicies.stream()
                .map(policy -> cleanExpiredEntities(policy, now, dryRun))
                .toList();
        return new DatabaseCleaningResponse(dryRun, entityCleaningResults);
    }

    private EntityCleaningResult cleanExpiredEntities(DatabaseEntityCleaningPolicy policy, LocalDateTime now, boolean dryRun) {
        try {
            Integer cleanedEntityCount = transactionTemplate.execute(status -> policy.cleanExpiredEntities(now, dryRun));
            log.info("Cleaned {} expired {} entities (dry run: {})", cleanedEntityCount, policy.getEntityName(), dryRun);
            return new EntityCleaningResult(policy.getEntityName(), cleanedEntityCount == null ? 0 : cleanedEntityCount, false);
        } catch (Exception e) {
            log.error("Cleaning expired {} entities failed", policy.getEntityName(), e);
            return new EntityCleaningResult(policy.getEntityName(), 0, true);
        }
    }
}
