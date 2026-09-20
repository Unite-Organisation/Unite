package com.app.prod.user;

import com.app.prod.access.BuildingScope;
import com.app.prod.access.TestBuildingScope;
import com.app.prod.authorization.service.ActivationService;
import com.app.prod.builders.AreaPersistenceFactory;
import com.app.prod.builders.BuildingPersistenceFactory;
import com.app.prod.builders.UserPersistanceFactory;
import com.app.prod.config.IntegrationTest;
import com.app.prod.config.MutableClock;
import com.app.prod.eventbus.EventExecutor;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.mail.MailMessage;
import com.app.prod.mail.MailSender;
import com.app.prod.mail.dto.EmailDeliveryResponse;
import com.app.prod.mail.enums.EmailDeliveryStatus;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.mail.repository.EmailDeliveryRepository;
import com.app.prod.user.dto.BulkCreationRequest;
import com.app.prod.user.dto.BulkCreationResponse;
import com.app.prod.user.dto.CreatedAccount;
import com.app.prod.user.dto.UserActivateRequest;
import com.app.prod.user.enums.SkipReason;
import com.app.prod.user.enums.UserRole;
import com.app.prod.user.enums.UserStatus;
import com.app.prod.user.repository.UserRepository;
import com.app.prod.user.service.PendingAccountService;
import org.jooq.sources.tables.records.AppUserRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSendException;

import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;

@SpringBootTest
@Import(PendingAccountInvitationIT.SynchronousMailConfig.class)
class PendingAccountInvitationIT extends IntegrationTest {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired
    private PendingAccountService pendingAccountService;
    @Autowired
    private ActivationService activationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailDeliveryRepository emailDeliveryRepository;
    @Autowired
    private UserPersistanceFactory userPersistanceFactory;
    @Autowired
    private AreaPersistenceFactory areaPersistenceFactory;
    @Autowired
    private BuildingPersistenceFactory buildingPersistenceFactory;
    @Autowired
    private RecordingMailSender mailSender;
    @Autowired
    private MutableClock clock;

    private UUID buildingId;
    private BuildingScope managerScope;

    @BeforeEach
    void setUp() {
        mailSender.clear();
        mailSender.rejectAll(false);

        var area = areaPersistenceFactory.getNewArea().withRandomValues().buildAndSave();
        buildingId = buildingPersistenceFactory.getNewBuilding().withRandomValues().areaId(area.getId()).buildAndSave().getId();
        UUID managerId = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave().getId();

        managerScope = TestBuildingScope.of(buildingId, managerId, UserRole.MANAGER);
    }

    @Test
    void createsALockedAccountPerAddressAndMailsEachOneALink() {
        String first = email();
        String second = email();

        BulkCreationResponse response = invite(first, second);

        assertThat(response.skipped()).isEmpty();
        assertThat(response.created()).extracting(CreatedAccount::email).containsExactlyInAnyOrder(first, second);

        AppUserRecord account = accountFor(first);
        assertThat(account.getStatus()).isEqualTo(UserStatus.CREATED.name());
        assertThat(account.getBuildingId()).isEqualTo(buildingId);
        assertThat(account.getUsername()).startsWith("pending-");
        assertThat(account.getFirstName()).isNull();

        assertThat(mailSender.sent()).hasSize(2);
        assertThat(mailSender.sent()).allSatisfy(message -> assertThat(tokenFrom(message)).isNotBlank());
    }

    @Test
    void everyInvitationIsRecordedAsADelivery() {
        String address = email();

        invite(address);

        List<EmailDeliveryResponse> deliveries = emailDeliveryRepository.findDeliveries(null).stream()
                .filter(delivery -> delivery.email().equals(address))
                .toList();

        assertThat(deliveries).singleElement().satisfies(delivery -> {
            assertThat(delivery.deliveryType()).isEqualTo(EmailDeliveryType.USER_CREATION);
            assertThat(delivery.status()).isEqualTo(EmailDeliveryStatus.SENT);
        });
    }

    @Test
    void addressesThatAlreadyHaveAnAccountAreSkipped() {
        String taken = userPersistanceFactory.getNewUser().withRandomValues().buildAndSave().getEmail();
        String fresh = email();

        BulkCreationResponse response = invite(taken, fresh);

        assertThat(response.created()).extracting(CreatedAccount::email).containsExactly(fresh);
        assertThat(response.skipped()).singleElement().satisfies(skipped -> {
            assertThat(skipped.email()).isEqualTo(taken.toLowerCase());
            assertThat(skipped.reason()).isEqualTo(SkipReason.EMAIL_ALREADY_USED);
        });
        assertThat(mailSender.sent()).hasSize(1);
    }

    @Test
    void theSameAddressPastedTwiceCreatesOneAccount() {
        String address = email();

        BulkCreationResponse response = invite(address, address.toUpperCase());

        assertThat(response.created()).hasSize(1);
        assertThat(response.skipped()).singleElement()
                .satisfies(skipped -> assertThat(skipped.reason()).isEqualTo(SkipReason.DUPLICATED_IN_REQUEST));
        assertThat(mailSender.sent()).hasSize(1);
    }

    @Test
    void anInvitationThatNeverArrivedIsRetriedWithoutASecondAccount() {
        String address = email();
        mailSender.rejectAll(true);
        UUID userId = invite(address).created().getFirst().userId();

        mailSender.rejectAll(false);
        mailSender.clear();
        BulkCreationResponse retry = invite(address);

        assertThat(retry.created()).isEmpty();
        assertThat(retry.skipped()).isEmpty();
        assertThat(retry.reinvited()).singleElement().satisfies(account -> {
            assertThat(account.email()).isEqualTo(address);
            assertThat(account.userId()).isEqualTo(userId);
        });
        assertThat(accountFor(address).getId()).isEqualTo(userId);

        assertThat(mailSender.sent()).hasSize(1);
        activationService.activate(activation(onlyToken(), "retried-" + UUID.randomUUID().toString().substring(0, 6)));
        assertThat(accountFor(address).getStatus()).isEqualTo(UserStatus.ACTIVE.name());
    }

    @Test
    void bothInvitationsAreRecordedAsSeparateDeliveries() {
        String address = email();
        mailSender.rejectAll(true);
        invite(address);

        mailSender.rejectAll(false);
        invite(address);

        List<EmailDeliveryResponse> deliveries = emailDeliveryRepository.findDeliveries(null).stream()
                .filter(delivery -> delivery.email().equals(address))
                .toList();

        assertThat(deliveries).hasSize(2);
        assertThat(deliveries).extracting(EmailDeliveryResponse::status)
                .containsExactlyInAnyOrder(EmailDeliveryStatus.FAILED, EmailDeliveryStatus.SENT);
    }

    @Test
    void anInvitationThatDidArriveIsNotRepeated() {
        String address = email();
        invite(address);
        mailSender.clear();

        BulkCreationResponse retry = invite(address);

        assertThat(retry.created()).isEmpty();
        assertThat(retry.reinvited()).isEmpty();
        assertThat(retry.skipped()).singleElement()
                .satisfies(skipped -> assertThat(skipped.reason()).isEqualTo(SkipReason.INVITATION_ALREADY_SENT));
        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    void aPendingAccountNobodyEverMailedGetsAnInvitation() {
        var pending = userPersistanceFactory.getNewUser().withRandomValues()
                .buildingId(buildingId)
                .status(UserStatus.CREATED.name())
                .buildAndSave();

        BulkCreationResponse response = invite(pending.getEmail());

        assertThat(response.reinvited()).extracting(account -> account.userId()).containsExactly(pending.getId());
        assertThat(mailSender.sent()).hasSize(1);
    }

    @Test
    void invitedPersonActivatesTheAccountWithTheLinkedToken() {
        String address = email();
        invite(address);
        String username = "activated-" + UUID.randomUUID().toString().substring(0, 6);

        activationService.activate(activation(onlyToken(), username));

        AppUserRecord account = accountFor(address);
        assertThat(account.getStatus()).isEqualTo(UserStatus.ACTIVE.name());
        assertThat(account.getUsername()).isEqualTo(username);
        assertThat(account.getFirstName()).isEqualTo("Jan");
        assertThat(account.getLastName()).isEqualTo("Kowalski");
        assertThat(account.getPassword()).startsWith("$2a$");
    }

    @Test
    void theSameLinkCannotActivateTwice() {
        invite(email());
        String token = onlyToken();
        activationService.activate(activation(token, "first-" + UUID.randomUUID().toString().substring(0, 6)));

        assertThatThrownBy(() -> activationService.activate(activation(token, "second-user")))
                .isInstanceOf(BadRequestException.class)
                .asInstanceOf(throwable(BadRequestException.class))
                .satisfies(exception -> assertThat(exception.getAppErrors().getFirst().code())
                        .isEqualTo(Code.ACTIVATION_TOKEN_USED));
    }

    @Test
    void anExpiredLinkIsRejected() {
        invite(email());
        String token = onlyToken();

        clock.advance(Duration.ofDays(8));

        assertThatThrownBy(() -> activationService.activate(activation(token, "late-user")))
                .asInstanceOf(throwable(BadRequestException.class))
                .satisfies(exception -> assertThat(exception.getAppErrors().getFirst().code())
                        .isEqualTo(Code.ACTIVATION_TOKEN_EXPIRED));
    }

    @Test
    void aMadeUpTokenIsRejected() {
        assertThatThrownBy(() -> activationService.activate(activation("not-a-real-token", "intruder")))
                .asInstanceOf(throwable(BadRequestException.class))
                .satisfies(exception -> assertThat(exception.getAppErrors().getFirst().code())
                        .isEqualTo(Code.ACTIVATION_TOKEN_INVALID));
    }

    private BulkCreationResponse invite(String... emails) {
        return pendingAccountService.createPendingAccounts(managerScope, new BulkCreationRequest(List.of(emails)));
    }

    private static UserActivateRequest activation(String token, String username) {
        return new UserActivateRequest(token, username, "Password123", "Jan", "Kowalski");
    }

    private String onlyToken() {
        assertThat(mailSender.sent()).hasSize(1);
        return tokenFrom(mailSender.sent().getFirst());
    }

    private static String tokenFrom(MailMessage message) {
        Matcher matcher = TOKEN_IN_LINK.matcher(message.body());
        assertThat(matcher.find()).as("activation link in %s", message.body()).isTrue();
        return matcher.group(1);
    }

    private AppUserRecord accountFor(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private static String email() {
        return UUID.randomUUID().toString().substring(0, 12) + "@example.com";
    }

    @TestConfiguration
    static class SynchronousMailConfig {

        @Bean
        @Primary
        EventExecutor sameThreadEventExecutor() {
            return Runnable::run;
        }

        @Bean
        @Primary
        RecordingMailSender recordingMailSender() {
            return new RecordingMailSender();
        }
    }

    static class RecordingMailSender implements MailSender {

        private final Queue<MailMessage> sent = new ConcurrentLinkedQueue<>();
        private volatile boolean rejectAll;

        @Override
        public void send(MailMessage message) {
            if (rejectAll) {
                throw new MailSendException("mailbox unavailable");
            }
            sent.add(message);
        }

        void rejectAll(boolean rejectAll) {
            this.rejectAll = rejectAll;
        }

        List<MailMessage> sent() {
            return List.copyOf(sent);
        }

        void clear() {
            sent.clear();
        }
    }
}
