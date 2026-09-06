package com.takarub.esim.commerce.infrastructure.notification;

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

import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.infrastructure.email.SmtpConfigService;

@ExtendWith(MockitoExtension.class)
class SmtpEsimReadyCustomerNotifierTest {

    @Mock
    private SmtpConfigService smtpConfigService;

    @Mock
    private JavaMailSender mailSender;

    private SmtpEsimReadyCustomerNotifier notifier;

    @BeforeEach
    void setUp() {
        when(smtpConfigService.getMailSender()).thenReturn(mailSender);
        when(smtpConfigService.getFromEmail()).thenReturn("noreply@takarub.example");
        notifier = new SmtpEsimReadyCustomerNotifier(smtpConfigService);
    }

    @Test
    void sendsSafeEsimReadyPlainTextWithoutActivationSecrets() {
        OrderId orderId = OrderId.of(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        EmailAddress recipient = EmailAddress.of("buyer@example.com");
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        notifier.notifyEsimReady(orderId, recipient);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("noreply@takarub.example");
        assertThat(message.getTo()).containsExactly("buyer@example.com");
        assertThat(message.getSubject()).isEqualTo("Your eSIM is ready");

        String body = message.getText();
        assertThat(body).contains("ready");
        assertThat(body).contains(orderId.value().toString());
        assertThat(body).containsIgnoringCase("My Orders");
        assertThat(body).containsIgnoringCase("activation");

        assertThat(body).doesNotContain("LPA:");
        assertThat(body).doesNotContain("activationCode");
        assertThat(body).doesNotContain("smdp");
        assertThat(body).doesNotContain("SMDP");
        assertThat(body).doesNotContain("ICCID");
        assertThat(body).doesNotContain("iccid");
        assertThat(body).doesNotContain("PIN");
        assertThat(body).doesNotContain("PUK");
        assertThat(body).doesNotContain("qrString");
        assertThat(body).doesNotContain("supplierOrderId");
        assertThat(body).doesNotContain("LIKE_CARD");
        assertThat(body).doesNotContain("remoteProductId");
    }
}
