package com.takarub.esim.identity.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.verification.VerificationId;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class LoggingNotificationSenderTest {

    private final LoggingNotificationSender sender = new LoggingNotificationSender();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(LoggingNotificationSender.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void logsEmailVerificationWithRecipientAndVerificationId() {
        EmailAddress recipient = EmailAddress.of("user@example.com");
        VerificationId verificationId = VerificationId.of(UUID.randomUUID());

        sender.sendEmailVerification(recipient, verificationId);

        assertThat(appender.list).hasSize(1);
        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("user@example.com");
        assertThat(message).contains(verificationId.value().toString());
    }

    @Test
    void logsPasswordResetWithRecipientAndVerificationId() {
        EmailAddress recipient = EmailAddress.of("reset@example.com");
        VerificationId verificationId = VerificationId.of(UUID.randomUUID());

        sender.sendPasswordResetInstructions(recipient, verificationId);

        assertThat(appender.list).hasSize(1);
        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message).contains("reset@example.com");
        assertThat(message).contains(verificationId.value().toString());
    }
}
