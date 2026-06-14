package com.takarub.esim.identity.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.takarub.esim.identity.application.port.NotificationSender;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.verification.VerificationId;

/**
 * Temporary {@link NotificationSender} implementation that logs notification intent instead of
 * delivering messages. A real provider (SMTP/transactional e-mail) adapter will replace this behind
 * the same port in a later task; link construction would move into that adapter.
 *
 * <p>Logs only the recipient and the {@link VerificationId} reference, never any secret material.
 */
@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void sendEmailVerification(EmailAddress recipient, VerificationId verificationId) {
        log.info("Email verification notification for recipient={} verificationId={}",
                recipient.value(), verificationId.value());
    }

    @Override
    public void sendPasswordResetInstructions(EmailAddress recipient, VerificationId verificationId) {
        log.info("Password reset notification for recipient={} verificationId={}",
                recipient.value(), verificationId.value());
    }
}
