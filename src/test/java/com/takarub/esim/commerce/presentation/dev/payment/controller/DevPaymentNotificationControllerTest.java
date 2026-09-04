package com.takarub.esim.commerce.presentation.dev.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.takarub.esim.commerce.application.command.HandlePaymentNotificationCommand;
import com.takarub.esim.commerce.application.exception.PaymentAttemptNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.PaymentVerificationDisposition;
import com.takarub.esim.commerce.application.result.PaymentVerificationView;
import com.takarub.esim.commerce.application.usecase.HandlePaymentNotificationUseCase;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.commerce.presentation.dev.payment.exception.DevPaymentNotificationExceptionHandler;
import com.takarub.esim.commerce.presentation.dev.payment.mapper.DevPaymentNotificationMapper;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;

@WebMvcTest(controllers = DevPaymentNotificationController.class)
@Import({
        DevPaymentNotificationMapper.class,
        DevPaymentNotificationExceptionHandler.class,
        GlobalExceptionHandler.class
})
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "takarub.commerce.dev-payment-verification.enabled=true")
class DevPaymentNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HandlePaymentNotificationUseCase handlePaymentNotificationUseCase;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SecurityContextProvider securityContextProvider;

    @Test
    void validSuccessNotificationReturns200AndMappedBody() throws Exception {
        String attemptId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        when(handlePaymentNotificationUseCase.execute(any())).thenReturn(view(
                attemptId, orderId, PaymentAttemptStatus.CONFIRMED, OrderStatus.PAID,
                PaymentVerificationDisposition.APPLIED));

        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"paymentAttemptId":"%s","verificationReference":"success"}
                                """.formatted(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentAttemptId").value(attemptId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.paymentAttemptStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.amount").value(9.99))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.disposition").value("APPLIED"));
    }

    @Test
    void validFailureNotificationReturns200() throws Exception {
        String attemptId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        when(handlePaymentNotificationUseCase.execute(any())).thenReturn(view(
                attemptId, orderId, PaymentAttemptStatus.FAILED, OrderStatus.PAYMENT_FAILED,
                PaymentVerificationDisposition.APPLIED));

        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"paymentAttemptId":"%s","verificationReference":"failure"}
                                """.formatted(attemptId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentAttemptStatus").value("FAILED"))
                .andExpect(jsonPath("$.orderStatus").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.disposition").value("APPLIED"));
    }

    @Test
    void passesExactPaymentAttemptIdAndVerificationReference() throws Exception {
        String attemptId = UUID.randomUUID().toString();
        String reference = "  success-with-spaces  ";
        when(handlePaymentNotificationUseCase.execute(any())).thenReturn(view(
                attemptId, UUID.randomUUID().toString(), PaymentAttemptStatus.CONFIRMED,
                OrderStatus.PAID, PaymentVerificationDisposition.APPLIED));

        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"paymentAttemptId":"%s","verificationReference":"%s"}
                                """.formatted(attemptId, reference)))
                .andExpect(status().isOk());

        ArgumentCaptor<HandlePaymentNotificationCommand> captor =
                ArgumentCaptor.forClass(HandlePaymentNotificationCommand.class);
        verify(handlePaymentNotificationUseCase).execute(captor.capture());
        assertThat(captor.getValue().paymentAttemptId()).isEqualTo(attemptId);
        assertThat(captor.getValue().verificationReference()).isEqualTo(reference);
    }

    @Test
    void missingPaymentAttemptIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("{\"verificationReference\":\"success\"}"))
                .andExpect(status().isBadRequest());
        verify(handlePaymentNotificationUseCase, never()).execute(any());
    }

    @Test
    void blankPaymentAttemptIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("{\"paymentAttemptId\":\" \",\"verificationReference\":\"success\"}"))
                .andExpect(status().isBadRequest());
        verify(handlePaymentNotificationUseCase, never()).execute(any());
    }

    @Test
    void missingVerificationReferenceReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("{\"paymentAttemptId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        verify(handlePaymentNotificationUseCase, never()).execute(any());
    }

    @Test
    void blankVerificationReferenceReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"paymentAttemptId":"%s","verificationReference":" "}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        verify(handlePaymentNotificationUseCase, never()).execute(any());
    }

    @Test
    void paymentAttemptNotFoundReturns404() throws Exception {
        PaymentAttemptId id = PaymentAttemptId.of(UUID.randomUUID());
        when(handlePaymentNotificationUseCase.execute(any()))
                .thenThrow(new PaymentAttemptNotFoundApplicationException(id));

        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"paymentAttemptId":"%s","verificationReference":"success"}
                                """.formatted(id.value())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMERCE_PAYMENT_ATTEMPT_NOT_FOUND"));
    }

    @Test
    void applicationConflictReturns409() throws Exception {
        when(handlePaymentNotificationUseCase.execute(any()))
                .thenThrow(new ConflictException("Verified amount does not match"));

        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"paymentAttemptId":"%s","verificationReference":"success"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void validationExceptionReturns400() throws Exception {
        when(handlePaymentNotificationUseCase.execute(any()))
                .thenThrow(new ValidationException("Unknown verification reference"));

        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"paymentAttemptId":"%s","verificationReference":"bogus"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void controllerDoesNotReadSecurityContextProvider() throws Exception {
        when(handlePaymentNotificationUseCase.execute(any())).thenReturn(view(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                PaymentAttemptStatus.CONFIRMED, OrderStatus.PAID,
                PaymentVerificationDisposition.APPLIED));

        mockMvc.perform(post("/api/v1/dev/payments/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"paymentAttemptId":"%s","verificationReference":"success"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk());

        verify(securityContextProvider, never()).currentUserId();
    }

    @Test
    void requestContractHasNoTrustedAmountCurrencyOrStatusFields() throws Exception {
        JsonNode schema = objectMapper.valueToTree(new com.takarub.esim.commerce.presentation.dev.payment
                .request.DevPaymentNotificationRequest("a", "b"));
        assertThat(schema.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "paymentAttemptId", "verificationReference");
        assertThat(schema.has("amount")).isFalse();
        assertThat(schema.has("currency")).isFalse();
        assertThat(schema.has("paymentStatus")).isFalse();
        assertThat(schema.has("orderStatus")).isFalse();
        assertThat(schema.has("success")).isFalse();
        assertThat(schema.has("markPaid")).isFalse();
        assertThat(schema.has("paid")).isFalse();
    }

    private static PaymentVerificationView view(String attemptId,
                                                String orderId,
                                                PaymentAttemptStatus attemptStatus,
                                                OrderStatus orderStatus,
                                                PaymentVerificationDisposition disposition) {
        return new PaymentVerificationView(
                PaymentAttemptId.of(attemptId),
                OrderId.of(orderId),
                attemptStatus,
                orderStatus,
                new BigDecimal("9.99"),
                "USD",
                disposition);
    }
}
