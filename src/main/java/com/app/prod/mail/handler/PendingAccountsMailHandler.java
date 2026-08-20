package com.app.prod.mail.handler;

import com.app.prod.authorization.config.ActivationProperties;
import com.app.prod.eventbus.EventHandler;
import com.app.prod.mail.config.MailProperties;
import com.app.prod.mail.dto.MailContent;
import com.app.prod.mail.dto.MailRecipient;
import com.app.prod.mail.enums.EmailDeliveryType;
import com.app.prod.mail.service.MailDeliveryService;
import com.app.prod.mail.template.MailTemplate;
import com.app.prod.mail.template.MailTemplateProcessor;
import com.app.prod.user.events.AccountInvitation;
import com.app.prod.user.events.PendingAccountsCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class PendingAccountsMailHandler implements EventHandler<PendingAccountsCreatedEvent> {

    private final MailDeliveryService mailDeliveryService;
    private final MailTemplateProcessor mailTemplateRenderer;
    private final MailProperties mailProperties;
    private final ActivationProperties activationProperties;

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

    private MailContent content(AccountInvitation invitation) {
        return mailTemplateRenderer.render(MailTemplate.USER_INVITATION, Map.of(
                "appName", mailProperties.getFromName(),
                "activationLink", invitation.activationLink(),
                "expiresInDays", activationProperties.getTokenTtl().toDays()
        ));
    }
}
