package com.app.prod.mail.template;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MailTemplate {

    USER_INVITATION("mail/invitation", "mail.invitation.subject");

    private final String view;
    private final String subjectKey;
}
