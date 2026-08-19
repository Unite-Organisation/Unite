package com.app.prod.mail.config;

import com.app.prod.mail.LoggingMailSender;
import com.app.prod.mail.MailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MailSenderConfig {

    @Bean
    @ConditionalOnMissingBean(MailSender.class)
    public MailSender fallbackMailSender() {
        log.warn("No mail provider configured - mails are only logged, nothing is actually sent");
        return new LoggingMailSender();
    }
}
