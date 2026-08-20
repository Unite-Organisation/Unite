package com.app.prod.mail;

import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.InternalConnectionException;
import com.app.prod.mail.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpMailSenderTest {

    private final JavaMailSenderImpl javaMailSender = mock(JavaMailSenderImpl.class);
    private SmtpMailSender smtpMailSender;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties();
        properties.setFrom("no-reply@unite.app");
        properties.setFromName("Unite");

        when(javaMailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        smtpMailSender = new SmtpMailSender(javaMailSender, properties);
    }

    @Test
    void sendsTheBodyAsHtmlFromTheConfiguredAddress() throws Exception {
        smtpMailSender.send(new MailMessage("resident@example.com", "Aktywacja", "<p>Cześć</p>"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();

        assertThat(sent.getFrom()[0].toString()).isEqualTo("Unite <no-reply@unite.app>");
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("resident@example.com");
        assertThat(sent.getSubject()).isEqualTo("Aktywacja");
        // headers are only written on saveChanges(), so the data handler is what carries the type here
        assertThat(sent.getDataHandler().getContentType()).contains("text/html");
        assertThat(sent.getContent()).isEqualTo("<p>Cześć</p>");
    }

    /**
     * A transport failure must arrive as an {@link com.app.prod.exceptions.exceptions.AppException}
     * subtype - that is what MailDeliveryService records as a FAILED delivery.
     */
    @Test
    void aTransportFailureBecomesAnAppException() {
        doThrow(new MailSendException("connection refused")).when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> smtpMailSender.send(new MailMessage("resident@example.com", "Aktywacja", "<p>Hi</p>")))
                .asInstanceOf(throwable(InternalConnectionException.class))
                .satisfies(exception -> {
                    assertThat(exception.getAppErrors().getFirst().code()).isEqualTo(Code.MAIL_SENDING_FAILED);
                    assertThat(exception.getAppErrors().getFirst().message()).contains("resident@example.com");
                });
    }
}
