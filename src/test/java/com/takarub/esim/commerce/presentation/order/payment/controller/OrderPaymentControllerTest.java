package com.takarub.esim.commerce.presentation.order.payment.controller;

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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.commerce.application.command.StartPaymentCommand;
import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.PaymentStartView;
import com.takarub.esim.commerce.application.usecase.StartPaymentUseCase;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.commerce.presentation.order.payment.exception.OrderPaymentExceptionHandler;
import com.takarub.esim.commerce.presentation.order.payment.mapper.OrderPaymentMapper;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ForbiddenException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;

@WebMvcTest(controllers = OrderPaymentController.class)
@Import({OrderPaymentMapper.class, OrderPaymentExceptionHandler.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class OrderPaymentControllerTest {

    private static final Instant FIXED = Instant.parse("2026-09-04T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StartPaymentUseCase startPaymentUseCase;

    @MockBean
    private SecurityContextProvider securityContextProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void authenticatedStartPaymentReturns200AndPassesExactCommand() throws Exception {
        String userId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(startPaymentUseCase.execute(any())).thenReturn(sampleView(orderId, true));

        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/start", orderId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentAttemptId").exists())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.paymentAttemptStatus").value("INITIATED"))
                .andExpect(jsonPath("$.orderStatus").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.amount").value(19.98))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.externalOrderId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.externalTransactionId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.created").value(true));

        ArgumentCaptor<StartPaymentCommand> captor = ArgumentCaptor.forClass(StartPaymentCommand.class);
        verify(startPaymentUseCase).execute(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
    }

    @Test
    void mapsReusedAttemptCreatedFalse() throws Exception {
        String userId = UUID.randomUUID().toString();
        String orderId = UUID.randomUUID().toString();
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(startPaymentUseCase.execute(any())).thenReturn(sampleView(orderId, false));

        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/start", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(false));
    }

    @Test
    void missingAuthenticationReturns401AndDoesNotCallUseCase() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/start", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(startPaymentUseCase, never()).execute(any());
    }

    @Test
    void orderNotFoundReturns404() throws Exception {
        String userId = UUID.randomUUID().toString();
        OrderId orderId = OrderId.of(UUID.randomUUID());
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(startPaymentUseCase.execute(any()))
                .thenThrow(new OrderNotFoundApplicationException(orderId));

        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/start", orderId.value().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMERCE_ORDER_NOT_FOUND"));
    }

    @Test
    void ownershipMismatchReturns403() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));
        when(startPaymentUseCase.execute(any()))
                .thenThrow(new ForbiddenException("Authenticated user does not own this order"));

        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/start", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void conflictReturns409() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));
        when(startPaymentUseCase.execute(any()))
                .thenThrow(new ConflictException("Order is already paid and cannot start payment"));

        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/start", UUID.randomUUID().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void malformedOrderIdReturns400FromValidationException() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));
        when(startPaymentUseCase.execute(any()))
                .thenThrow(new ValidationException("OrderId must be a valid UUID"));

        mockMvc.perform(post("/api/v1/orders/{orderId}/payment/start", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private static PaymentStartView sampleView(String orderId, boolean created) {
        return new PaymentStartView(
                PaymentAttemptId.of(UUID.randomUUID()),
                OrderId.of(orderId),
                PaymentAttemptStatus.INITIATED,
                OrderStatus.PENDING_PAYMENT,
                new BigDecimal("19.98"),
                "USD",
                null,
                null,
                FIXED,
                FIXED,
                created);
    }
}
