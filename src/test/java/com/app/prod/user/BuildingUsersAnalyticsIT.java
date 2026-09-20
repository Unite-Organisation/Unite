package com.app.prod.user;

import com.app.prod.access.BuildingScope;
import com.app.prod.access.TestBuildingScope;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.eventbus.EventExecutor;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.mail.MailMessage;
import com.app.prod.mail.MailSender;
import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.user.dto.BuildingUserFilterRequest;
import com.app.prod.user.dto.BuildingUserResponse;
import com.app.prod.user.dto.BulkCreationRequest;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.service.PendingAccountService;
import com.app.prod.user.service.UserFilteringService;
import com.app.prod.user.service.UserService;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.BuildingUserFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSendException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;

@SpringBootTest
@Import(BuildingUsersAnalyticsIT.UnreliableMailConfig.class)
class BuildingUsersAnalyticsIT extends IntegrationTest {

    @Autowired
    private PendingAccountService pendingAccountService;
    @Autowired
    private UserFilteringService userFilteringService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private UnreliableMailSender mailSender;
    @Autowired
    private UserRepository userRepository;

    private UUID buildingId;
    private BuildingScope managerScope;

    @BeforeEach
    void setUp() {
        mailSender.rejectAll(false);

        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        buildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();
        UUID managerId = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave().getId();

        managerScope = TestBuildingScope.of(buildingId, managerId, UserRole.MANAGER);
    }

    @Test
    void listsContactDetailsAndStatusOfEverybodyInTheBuilding() {
        var active = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave();
        String invited = invite();

        List<BuildingUserResponse> users = analyse(request().build());

        assertThat(users).extracting(BuildingUserResponse::email).containsExactlyInAnyOrder(active.getEmail(), invited);
        assertThat(byEmail(users, active.getEmail())).satisfies(user -> {
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.username()).isEqualTo(active.getUsername());
            assertThat(user.firstName()).isEqualTo(active.getFirstName());
        });
    }

    @Test
    void aPendingAccountCarriesTheInvitationResult() {
        String invited = invite();

        BuildingUserResponse user = byEmail(analyse(request().build()), invited);

        assertThat(user.status()).isEqualTo(UserStatus.CREATED);
        assertThat(user.invitation()).isNotNull();
        assertThat(user.invitation().status()).isEqualTo(EmailDeliveryStatus.SENT);
        assertThat(user.invitation().sentAt()).isNotNull();
        assertThat(user.invitation().errorMessage()).isNull();
    }

    @Test
    void aFailedInvitationKeepsTheReasonVisible() {
        mailSender.rejectAll(true);
        String invited = invite();

        BuildingUserResponse user = byEmail(analyse(request().build()), invited);

        assertThat(user.invitation().status()).isEqualTo(EmailDeliveryStatus.FAILED);
        assertThat(user.invitation().errorMessage()).contains("mailbox unavailable");
        assertThat(user.invitation().sentAt()).isNull();
    }

    /**
     * The case a manager is really after: an account waiting for activation that nobody ever mailed.
     */
    @Test
    void aPendingAccountWithoutAnyMailHasNoInvitation() {
        var pending = userPersistanceFactory.getNewUser().withRandomValues()
                .buildingId(buildingId)
                .status(UserStatus.CREATED.name())
                .buildAndSave();

        assertThat(byEmail(analyse(request().build()), pending.getEmail()).invitation()).isNull();
    }

    @Test
    void anActivatedAccountDropsTheInvitationDetails() {
        String invited = invite();
        UUID userId = byEmail(analyse(request().build()), invited).id();

        activate(userId);

        BuildingUserResponse user = byEmail(analyse(request().build()), invited);
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.invitation()).isNull();
    }

    @Test
    void nobodyFromAnotherBuildingLeaksIn() {
        var otherArea = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        var otherBuilding = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(otherArea.getId()).buildAndSave();
        var stranger = userPersistanceFactory.getNewUser().withRandomValues().buildingId(otherBuilding.getId()).buildAndSave();
        var neighbour = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave();

        List<BuildingUserResponse> users = analyse(request().build());

        assertThat(users).extracting(BuildingUserResponse::email)
                .contains(neighbour.getEmail())
                .doesNotContain(stranger.getEmail());
    }

    @Test
    void narrowsByStatus() {
        var active = userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave();
        String invited = invite();

        assertThat(analyse(request().status(UserStatus.CREATED).build()))
                .extracting(BuildingUserResponse::email).containsExactly(invited);
        assertThat(analyse(request().status(UserStatus.ACTIVE).build()))
                .extracting(BuildingUserResponse::email).containsExactly(active.getEmail());
    }

    @Test
    void narrowsByHowTheInvitationEnded() {
        String delivered = invite();
        mailSender.rejectAll(true);
        String rejected = invite();

        assertThat(analyse(request().invitationStatus(EmailDeliveryStatus.FAILED).build()))
                .extracting(BuildingUserResponse::email).containsExactly(rejected);
        assertThat(analyse(request().invitationStatus(EmailDeliveryStatus.SENT).build()))
                .extracting(BuildingUserResponse::email).containsExactly(delivered);
    }

    @Test
    void askingForActiveAccountsAndAnInvitationAtOnceIsRejected() {
        assertThatThrownBy(() -> analyse(request()
                .status(UserStatus.ACTIVE)
                .invitationStatus(EmailDeliveryStatus.SENT)
                .build()))
                .asInstanceOf(throwable(BadRequestException.class))
                .satisfies(exception -> assertThat(exception.getAppErrors().getFirst().code())
                        .isEqualTo(Code.CONFLICTING_FILTERS));
    }

    @Test
    void paginatesInsteadOfReturningTheWholeBuilding() {
        for (int i = 0; i < 6; i++) {
            userPersistanceFactory.getNewUser().withRandomValues().buildingId(buildingId).buildAndSave();
        }

        List<BuildingUserResponse> firstPage = analyse(request().pageSize(5).page(1).build());
        List<BuildingUserResponse> secondPage = analyse(request().pageSize(5).page(2).build());

        assertThat(firstPage).hasSize(5);
        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.getFirst().id()).isNotIn(firstPage.stream().map(BuildingUserResponse::id).toList());
    }

    private List<BuildingUserResponse> analyse(BuildingUserFilterRequest request) {
        BuildingUserFilter filter = userFilteringService.prepareFilter(managerScope, request);
        return userService.getUsersInBuilding(request.pagination(), filter);
    }

    private static BuildingUserFilterRequest.BuildingUserFilterRequestBuilder<?, ?> request() {
        return BuildingUserFilterRequest.builder();
    }

    private String invite() {
        String email = UUID.randomUUID().toString().substring(0, 12) + "@example.com";
        pendingAccountService.createPendingAccounts(managerScope, new BulkCreationRequest(List.of(email)));
        return email;
    }

    private void activate(UUID userId) {
        userRepository.activateUser(
                userId,
                "activated-" + UUID.randomUUID().toString().substring(0, 6),
                "$2a$10$hash",
                "Jan",
                "Kowalski"
        );
    }

    private static BuildingUserResponse byEmail(List<BuildingUserResponse> users, String email) {
        return users.stream()
                .filter(user -> email.equals(user.email()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no user with email " + email + " in " + users));
    }

    @TestConfiguration
    static class UnreliableMailConfig {

        @Bean
        @Primary
        EventExecutor sameThreadEventExecutor() {
            return Runnable::run;
        }

        @Bean
        @Primary
        UnreliableMailSender unreliableMailSender() {
            return new UnreliableMailSender();
        }
    }

    static class UnreliableMailSender implements MailSender {

        private volatile boolean rejectAll;

        @Override
        public void send(MailMessage message) {
            if (rejectAll) {
                throw new MailSendException("mailbox unavailable");
            }
        }

        void rejectAll(boolean rejectAll) {
            this.rejectAll = rejectAll;
        }
    }
}
