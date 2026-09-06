package com.takarub.esim.commerce.infrastructure.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.takarub.esim.commerce.application.port.EsimReadyCustomerNotifier;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.infrastructure.email.SmtpConfigService;

/**
 * Sends a plain-text eSIM-ready notification via database-driven SMTP. Does not include activation
 * secrets or supplier internals.
 */
public class SmtpEsimReadyCustomerNotifier implements EsimReadyCustomerNotifier {

    private static final Logger log = LoggerFactory.getLogger(SmtpEsimReadyCustomerNotifier.class);

    private final SmtpConfigService smtpConfigService;

    public SmtpEsimReadyCustomerNotifier(SmtpConfigService smtpConfigService) {
        this.smtpConfigService = smtpConfigService;
    }

    @Override
    public void notifyEsimReady(OrderId orderId, EmailAddress recipient) {
        String orderRef = orderId.value().toString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(smtpConfigService.getFromEmail());
        message.setTo(recipient.value());
        message.setSubject("Your eSIM is ready");
        message.setText("""
                Your Takarub eSIM is ready.

                Order reference: %s

                Open My Orders in your account to view activation details.
                """.formatted(orderRef));
        try {
            JavaMailSender mailSender = smtpConfigService.getMailSender();
            mailSender.send(message);
            log.info("Sent eSIM ready notification for orderId={}", orderRef);
        } catch (MailException ex) {
            log.error("Failed to send eSIM ready notification for orderId={}", orderRef, ex);
            throw ex;
        }
    }
}
