package com.takarub.esim.commerce.presentation.dev.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.commerce.application.result.PaymentVerificationDisposition;
import com.takarub.esim.commerce.application.result.PaymentVerificationView;
import com.takarub.esim.commerce.application.usecase.HandlePaymentNotificationUseCase;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.commerce.presentation.dev.payment.mapper.DevPaymentNotificationMapper;
import com.takarub.esim.identity.infrastructure.config.JwtProperties;
import com.takarub.esim.identity.infrastructure.config.SecurityConfig;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.security.AuthenticatedSessionValidator;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;

/**
 * Proves the exact conditional-enabled POST is permitAll and customer write endpoints remain
 * authenticated. Filters are enabled (unlike the mapping-focused controller test).
 */
@WebMvcTest(controllers = {
        DevPaymentNotificationController.class,
        DevPaymentNotificationSecurityIT.ProtectedEndpointStubs.class
})
@Import({
        DevPaymentNotificationMapper.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "takarub.commerce.dev-payment-verification.enabled=true",
        "identity.jwt.secret=test-secret-key-at-least-32-characters-long!!",
        "identity.jwt.issuer=test-issuer",
        "identity.jwt.access-token-expiration=15m"
})
class DevPaymentNotificationSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HandlePaymentNotificationUseCase handlePaymentNotificationUseCase;

    @MockBean
    private JwtAccessTokenValidator jwtAccessTokenValidator;

    @MockBean
    private AuthenticatedSessionValidator authenticatedSessionValidator;

    @MockBean
    private LoggingAuditEventRecorder loggingAuditEventRecorder;

    @MockBean
    private JwtProperties jwtProperties;

    @Test
    void enabledDevNotificationPostDoesNotRequireCustomerAuthentication() throws Exception {
        when(handlePaymentNotificationUseCase.execute(any())).thenReturn(new PaymentVerificationView(
                PaymentAttemptId.of(UUID.randomUUID()),
                OrderId.of(UUID.randomUUID()),
                PaymentAttemptStatus.CONFIRMED,
                OrderStatus.PAID,
                new BigDecimal("9.99"),
                "USD",
                PaymentVerificationDisposition.APPLIED));

        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentAttemptId":"%s","verificationReference":"success"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());
    }

    @Test
    void checkoutRemainsAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/checkout").with(anonymous()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void paymentStartRetryRemainsAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/start", UUID.randomUUID())
                        .with(anonymous()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @org.springframework.web.bind.annotation.RestController
    static class ProtectedEndpointStubs {
        @org.springframework.web.bind.annotation.PostMapping("/api/v1/checkout")
        String checkout() {
            return "ok";
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/orders/{orderId}/payment/start")
        String start(@org.springframework.web.bind.annotation.PathVariable String orderId) {
            return orderId;
        }
    }
}
