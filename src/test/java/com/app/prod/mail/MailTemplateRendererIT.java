package com.app.prod.mail;

import com.app.prod.config.IntegrationTest;
import com.app.prod.mail.dto.MailContent;
import com.app.prod.mail.template.MailTemplate;
import com.app.prod.mail.template.MailTemplateProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MailTemplateRendererIT extends IntegrationTest {

    private static final String EVENT_LINK = "http://localhost:4204/event/abc123";
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

    @Test
    void everyEventMailRendersInBothLanguagesWithNoKeyLeftUnresolved() {
        for (MailTemplate template : List.of(MailTemplate.EVENT_GROUP_FORMED, MailTemplate.EVENT_CONFIRMED, MailTemplate.EVENT_REDUCED_TO_ONE_SLOT)) {
            for (Locale locale : List.of(Locale.of("pl"), Locale.ENGLISH)) {
                MailContent content = mailTemplateRenderer.render(template, locale, eventVariables());

                assertThat(content.subject()).as("%s subject in %s", template, locale).isNotBlank().doesNotContain("??");
                assertThat(content.body()).as("%s body in %s", template, locale)
                        .startsWith("<!DOCTYPE html>")
                        .contains("Volleyball", EVENT_LINK, "Hala sportowa", "Poniedzialek 18:00")
                        .doesNotContain("??");
            }
        }
    }

    @Test
    void anEventMailLeavesOutThePlaceItDoesNotHave() {
        Map<String, Object> variables = new HashMap<>(eventVariables());
        variables.put("location", null);
        variables.put("onlineUrl", null);

        MailContent content = mailTemplateRenderer.render(MailTemplate.EVENT_CONFIRMED, Locale.ENGLISH, variables);

        assertThat(content.body()).doesNotContain("Where").doesNotContain("Online");
    }

    private Map<String, Object> eventVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("appName", "Unitly");
        variables.put("eventName", "Volleyball");
        variables.put("eventLink", EVENT_LINK);
        variables.put("when", "Poniedzialek 18:00");
        variables.put("location", "Hala sportowa");
        variables.put("onlineUrl", "https://meet.example.com/abc");
        variables.put("confirmBy", "Poniedzialek 19:00");
        variables.put("minAttendees", 4);
        return variables;
    }

    private MailContent render(Locale locale) {
        return mailTemplateRenderer.render(MailTemplate.USER_INVITATION, locale, Map.of(
                "appName", "Unite",
                "activationLink", LINK,
                "expiresInDays", 7L
        ));
    }
}
