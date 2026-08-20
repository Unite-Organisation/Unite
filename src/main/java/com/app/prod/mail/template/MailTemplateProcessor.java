package com.app.prod.mail.template;

import com.app.prod.mail.config.MailProperties;
import com.app.prod.mail.dto.MailContent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailTemplateProcessor {

    private final SpringTemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final MailProperties mailProperties;

    public MailContent render(MailTemplate template, Map<String, Object> variables) {
        return render(template, mailProperties.getLocale(), variables);
    }

    public MailContent render(MailTemplate template, Locale locale, Map<String, Object> variables) {
        Context context = new Context(locale, variables);
        String subject = messageSource.getMessage(template.getSubjectKey(), null, locale);

        return new MailContent(subject, templateEngine.process(template.getView(), context));
    }
}
