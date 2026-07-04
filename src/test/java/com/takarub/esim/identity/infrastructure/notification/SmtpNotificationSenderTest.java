package com.takarub.esim.identity.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.infrastructure.email.SmtpConfigService;

@ExtendWith(MockitoExtension.class)
class SmtpNotificationSenderTest {

    @Mock
    private SmtpConfigService smtpConfigService;

    @Mock
    private JavaMailSender mailSender;

    private SmtpNotificationSender sender;

    @BeforeEach
    void setUp() {
        when(smtpConfigService.getMailSender()).thenReturn(mailSender);
        when(smtpConfigService.getFromEmail()).thenReturn("sender@example.com");
        sender = new SmtpNotificationSender(smtpConfigService);
    }

    @Test
    void sendsEmailVerificationWithTokenInBody() {
        EmailAddress recipient = EmailAddress.of("user@example.com");
        VerificationId verificationId = VerificationId.of(UUID.randomUUID());
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        sender.sendEmailVerification(recipient, verificationId);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("sender@example.com");
        assertThat(message.getTo()).containsExactly("user@example.com");
        assertThat(message.getSubject()).contains("Verify");
        assertThat(message.getText()).contains(verificationId.value().toString());
    }

    @Test
    void sendsPasswordResetWithTokenInBody() {
        EmailAddress recipient = EmailAddress.of("reset@example.com");
        VerificationId verificationId = VerificationId.of(UUID.randomUUID());
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        sender.sendPasswordResetInstructions(recipient, verificationId);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("reset@example.com");
        assertThat(message.getSubject()).contains("Reset");
        assertThat(message.getText()).contains(verificationId.value().toString());
    }
}
