package com.app.prod.mail.handler;

import com.app.prod.eventbus.EventHandler;
import com.app.prod.mail.dto.MailContent;
import com.app.prod.mail.dto.MailRecipient;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.mail.service.MailDeliveryService;
import com.app.prod.user.events.AccountInvitation;
import com.app.prod.user.events.PendingAccountsCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class PendingAccountsMailHandler implements EventHandler<PendingAccountsCreatedEvent> {

    private final MailDeliveryService mailDeliveryService;

    @Override
    public void handle(PendingAccountsCreatedEvent event) {
        log.info("Sending {} invitations for building {}", event.invitations().size(), event.buildingId());

        for (AccountInvitation invitation : event.invitations()) {
            mailDeliveryService.send(
                    EmailDeliveryType.USER_CREATION,
                    invitation.tokenId(),
                    List.of(new MailRecipient(invitation.userId(), invitation.email())),
                    content(invitation)
            );
        }
    }

    @Override
    public Class<PendingAccountsCreatedEvent> eventType() {
        return PendingAccountsCreatedEvent.class;
    }

    private static MailContent content(AccountInvitation invitation) {
        return new MailContent(
                "Your account is ready to be activated",
                String.format("An account was created for you. Set it up here: %s", invitation.activationLink())
        );
    }
}
