package com.app.prod.user.service;

import com.app.prod.access.BuildingScope;
import com.app.prod.authorization.dto.IssuedActivationToken;
import com.app.prod.authorization.service.ActivationTokenService;
import com.app.prod.eventbus.EventBus;
import com.app.prod.user.dto.BulkCreationRequest;
import com.app.prod.user.dto.BulkCreationResponse;
import com.app.prod.user.dto.CreatedAccount;
import com.app.prod.user.dto.ExistingAccount;
import com.app.prod.user.dto.ReinvitedAccount;
import com.app.prod.user.dto.SkippedEmail;
import com.app.prod.user.enums.SkipReason;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.events.AccountInvitation;
import com.app.prod.user.events.AccountsInvitedEvent;
import com.app.prod.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.sources.tables.records.AppUserRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        Map<String, ExistingAccount> existing = findExisting(requested);

        List<ReinvitedAccount> reinvited = new ArrayList<>();
        List<String> toCreate = new ArrayList<>();

        for (String email : requested) {
            ExistingAccount account = existing.get(email);

            if (account == null) {
                toCreate.add(email);
            } else if (account.awaitsAnInvitationThatNeverArrived()) {
                reinvited.add(new ReinvitedAccount(account.userId(), account.email()));
            } else {
                skipped.add(new SkippedEmail(email, skipReasonFor(account)));
            }
        }

        List<AppUserRecord> accounts = toCreate.stream()
                .map(email -> pendingAccount(email, scope.buildingId()))
                .toList();

        if (accounts.isEmpty() && reinvited.isEmpty()) {
            log.info("Nothing to invite in building {}, all {} addresses skipped", scope.buildingId(), skipped.size());
            return new BulkCreationResponse(List.of(), List.of(), skipped);
        }

        if (!accounts.isEmpty()) {
            userRepository.insertMany(accounts);
        }

        publishInvitations(scope, accounts, reinvited);

        log.info("Building {}: created {} pending accounts, re-invited {}, skipped {}",
                scope.buildingId(), accounts.size(), reinvited.size(), skipped.size());
        return new BulkCreationResponse(
                accounts.stream().map(account -> new CreatedAccount(account.getId(), account.getEmail())).toList(),
                reinvited,
                skipped
        );
    }

    private Map<String, ExistingAccount> findExisting(Set<String> requested) {
        return userRepository.findAccountsByEmails(requested).stream()
                .collect(Collectors.toMap(ExistingAccount::email, Function.identity()));
    }

    private static SkipReason skipReasonFor(ExistingAccount account) {
        return account.status() == UserStatus.ACTIVE ? SkipReason.EMAIL_ALREADY_USED : SkipReason.INVITATION_ALREADY_SENT;
    }

    private void publishInvitations(BuildingScope scope, List<AppUserRecord> accounts, List<ReinvitedAccount> reinvited) {
        Map<UUID, String> emailsByUser = new LinkedHashMap<>();
        accounts.forEach(account -> emailsByUser.put(account.getId(), account.getEmail()));
        reinvited.forEach(account -> emailsByUser.put(account.userId(), account.email()));

        List<IssuedActivationToken> tokens = activationTokenService.issueFor(List.copyOf(emailsByUser.keySet()));
        List<AccountInvitation> invitations = tokens.stream()
                .map(token -> new AccountInvitation(
                        token.userId(),
                        token.tokenId(),
                        emailsByUser.get(token.userId()),
                        token.activationLink()
                ))
                .toList();

        eventBus.publish(new AccountsInvitedEvent(scope.buildingId(), invitations));
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
