package com.takarub.esim.identity.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.takarub.esim.identity.application.port.NotificationSender;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.infrastructure.email.SmtpConfigService;

/**
 * Delivers identity notifications through SMTP using database-driven configuration.
 * The {@link SmtpConfigService} provides a cached {@link JavaMailSender} built from
 * the active SMTP configuration stored in the database.
 */
public class SmtpNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpNotificationSender.class);

    private final SmtpConfigService smtpConfigService;

    public SmtpNotificationSender(SmtpConfigService smtpConfigService) {
        this.smtpConfigService = smtpConfigService;
    }

    @Override
    public void sendEmailVerification(EmailAddress recipient, VerificationId verificationId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(smtpConfigService.getFromEmail());
        message.setTo(recipient.value());
        message.setSubject("Verify your Takarub eSIM account");
        message.setText("""
                Welcome to Takarub eSIM!

                Your email verification token:
                %s

                Use it with POST /api/v1/auth/verify-email:
                { "token": "%s" }

                This token expires in 24 hours.
                """.formatted(verificationId.value(), verificationId.value()));
        send(message, recipient, verificationId, "email verification");
    }

    @Override
    public void sendPasswordResetInstructions(EmailAddress recipient, VerificationId verificationId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(smtpConfigService.getFromEmail());
        message.setTo(recipient.value());
        message.setSubject("Reset your Takarub eSIM password");
        message.setText("""
                You requested a password reset for your Takarub eSIM account.

                Your reset token:
                %s

                Use it with POST /api/v1/auth/password/reset:
                { "token": "%s", "newPassword": "<your-new-password>" }

                This token expires in 1 hour. If you did not request this, ignore this email.
                """.formatted(verificationId.value(), verificationId.value()));
        send(message, recipient, verificationId, "password reset");
    }

    private void send(SimpleMailMessage message, EmailAddress recipient,
                      VerificationId verificationId, String notificationType) {
        try {
            JavaMailSender mailSender = smtpConfigService.getMailSender();
            mailSender.send(message);
            log.info("Sent {} notification to recipient={} verificationId={}",
                    notificationType, recipient.value(), verificationId.value());
        } catch (MailException ex) {
            log.error("Failed to send {} notification to recipient={} verificationId={}",
                    notificationType, recipient.value(), verificationId.value(), ex);
            throw ex;
        }
    }
}
