package com.app.prod.mail;

import com.app.prod.config.IntegrationTest;
import com.app.prod.mail.dto.MailContent;
import com.app.prod.mail.template.MailTemplate;
import com.app.prod.mail.template.MailTemplateProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MailTemplateRendererIT extends IntegrationTest {

    private static final String LINK = "http://localhost:3000/activate?token=Ab-9_x";

    @Autowired
    private MailTemplateProcessor mailTemplateRenderer;

    @Test
    void rendersTheInvitationInEverySupportedLanguage() {
        MailContent polish = render(Locale.of("pl"));
        MailContent english = render(Locale.ENGLISH);

        assertThat(polish.subject()).isNotBlank();
        assertThat(english.subject()).isNotBlank();
        assertThat(polish.subject()).isNotEqualTo(english.subject());

        assertThat(polish.body()).contains(LINK).contains("Unite").contains("7");
        assertThat(english.body()).contains(LINK).contains("Unite").contains("7");
    }

    /**
     * Thymeleaf prints a missing translation as {@code ??key_locale??} instead of failing, so a
     * forgotten key would only show up in somebody's mailbox. This is what catches it in the build.
     */
    @Test
    void noTranslationKeyIsLeftUnresolved() {
        assertThat(render(Locale.of("pl")).body()).doesNotContain("??");
        assertThat(render(Locale.ENGLISH).body()).doesNotContain("??");
    }

    @Test
    void theLayoutWrapsTheContentInAFullHtmlDocument() {
        String body = render(Locale.of("pl")).body();

        assertThat(body).startsWith("<!DOCTYPE html>");
        assertThat(body).contains("<html").contains("</html>");
    }

    private MailContent render(Locale locale) {
        return mailTemplateRenderer.render(MailTemplate.USER_INVITATION, locale, Map.of(
                "appName", "Unite",
                "activationLink", LINK,
                "expiresInDays", 7L
        ));
    }
}
