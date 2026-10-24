package com.app.prod.mail.template;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MailTemplate {

    USER_INVITATION("mail/invitation", "mail.invitation.subject"),
    EVENT_GROUP_FORMED("mail/event-group-formed", "mail.event.group-formed.subject"),
    EVENT_GROUP_BROKE_UP("mail/event-group-broke-up", "mail.event.group-broke-up.subject"),
    EVENT_CONFIRMED("mail/event-confirmed", "mail.event.confirmed.subject"),
    EVENT_REDUCED_TO_ONE_SLOT("mail/event-reduced-to-one-slot", "mail.event.reduced-to-one-slot.subject");

    private final String view;
    private final String subjectKey;
}
