package com.app.prod.user.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.authorization.dto.IssuedActivationToken;
import com.app.prod.authorization.service.ActivationTokenService;
import com.app.prod.eventbus.EventBus;
import com.app.prod.user.dto.BulkCreationRequest;
import com.app.prod.user.dto.BulkCreationResponse;
import com.app.prod.user.dto.CreatedAccount;
import com.app.prod.user.dto.SkippedEmail;
import com.app.prod.user.enums.SkipReason;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.events.AccountInvitation;
import com.app.prod.user.events.PendingAccountsCreatedEvent;
import com.app.prod.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingAccountService {

    private static final String LOCKED_PASSWORD = "LOCKED";
    private static final String PLACEHOLDER_USERNAME_PREFIX = "pending-";

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final ActivationTokenService activationTokenService;
    private final EventBus eventBus;
    private final Clock clock;

    @Transactional
    public BulkCreationResponse createPendingAccounts(BuildingScope scope, BulkCreationRequest request) {
        List<SkippedEmail> skipped = new ArrayList<>();
        Set<String> requested = deduplicate(request.emails(), skipped);
        Set<String> taken = userRepository.findExistingEmails(requested);

        List<String> toCreate = requested.stream()
                .filter(email -> {
                    boolean alreadyUsed = taken.contains(email);
                    if (alreadyUsed) {
                        skipped.add(new SkippedEmail(email, SkipReason.EMAIL_ALREADY_USED));
                    }
                    return !alreadyUsed;
                })
                .toList();

        if (toCreate.isEmpty()) {
            log.info("No accounts to create in building {}, all {} addresses skipped", scope.buildingId(), skipped.size());
            return new BulkCreationResponse(List.of(), skipped);
        }

        List<AppUserRecord> accounts = toCreate.stream()
                .map(email -> pendingAccount(email, scope.buildingId()))
                .toList();
        userRepository.insertMany(accounts);

        publishInvitations(scope, accounts);

        log.info("Created {} pending accounts in building {}, skipped {}", accounts.size(), scope.buildingId(), skipped.size());
        return new BulkCreationResponse(
                accounts.stream().map(account -> new CreatedAccount(account.getId(), account.getEmail())).toList(),
                skipped
        );
    }

    private void publishInvitations(BuildingScope scope, List<AppUserRecord> accounts) {
        Map<UUID, String> emailsByUser = accounts.stream()
                .collect(Collectors.toMap(AppUserRecord::getId, AppUserRecord::getEmail));

        List<IssuedActivationToken> tokens = activationTokenService.issueFor(List.copyOf(emailsByUser.keySet()));
        List<AccountInvitation> invitations = tokens.stream()
                .map(token -> new AccountInvitation(
                        token.userId(),
                        token.tokenId(),
                        emailsByUser.get(token.userId()),
                        token.activationLink()
                ))
                .toList();

        eventBus.publish(new PendingAccountsCreatedEvent(scope.buildingId(), invitations));
    }

    private static Set<String> deduplicate(List<String> emails, List<SkippedEmail> skipped) {
        Set<String> unique = new LinkedHashSet<>();

        for (String email : emails) {
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (!unique.add(normalized)) {
                skipped.add(new SkippedEmail(normalized, SkipReason.DUPLICATED_IN_REQUEST));
            }
        }

        return unique;
    }

    private AppUserRecord pendingAccount(String email, UUID buildingId) {
        return new AppUserRecord(
                UUID.randomUUID(),
                null,
                null,
                email,
                placeholderUsername(),
                LOCKED_PASSWORD,
                userRoleService.getUserRoleId(UserRole.RESIDENT),
                UserStatus.CREATED.name(),
                LocalDateTime.now(clock),
                buildingId
        );
    }

    private static String placeholderUsername() {
        return PLACEHOLDER_USERNAME_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
