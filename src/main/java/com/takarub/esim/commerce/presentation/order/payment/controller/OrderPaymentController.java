package com.takarub.esim.commerce.presentation.order.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.commerce.application.command.StartPaymentCommand;
import com.takarub.esim.commerce.application.usecase.StartPaymentUseCase;
import com.takarub.esim.commerce.presentation.order.payment.mapper.OrderPaymentMapper;
import com.takarub.esim.commerce.presentation.order.payment.response.PaymentStartResponse;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Explicit start/retry payment for an existing Order (Pay Again after {@code PAYMENT_FAILED}, or
 * recovery reuse of an active {@code INITIATED} attempt). Initial purchase remains
 * {@code POST /api/v1/checkout}.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Payment", description = "Authenticated explicit payment start / retry for an existing Order")
public class OrderPaymentController {

    private final StartPaymentUseCase startPaymentUseCase;
    private final OrderPaymentMapper orderPaymentMapper;
    private final SecurityContextProvider securityContextProvider;

    public OrderPaymentController(StartPaymentUseCase startPaymentUseCase,
                                  OrderPaymentMapper orderPaymentMapper,
                                  SecurityContextProvider securityContextProvider) {
        this.startPaymentUseCase = startPaymentUseCase;
        this.orderPaymentMapper = orderPaymentMapper;
        this.securityContextProvider = securityContextProvider;
    }

    @PostMapping("/{orderId}/payment/start")
    @Operation(
            summary = "Start or retry payment for an existing Order",
            description = "Explicit payment start for an Order owned by the authenticated user. "
                    + "Uses server-owned Order amount and currency (never from the client). "
                    + "When the Order is PENDING_PAYMENT with an active INITIATED attempt, that "
                    + "attempt is reused. When the Order is PAYMENT_FAILED with no active attempt, "
                    + "a new PaymentAttempt is created and the Order returns to PENDING_PAYMENT. "
                    + "PAID and other conflicting states are rejected. "
                    + "This is not the initial purchase path — use POST /api/v1/checkout for that. "
                    + "No request body; no Idempotency-Key; no PSP-specific fields.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment attempt created or existing active attempt reused",
                    content = @Content(schema = @Schema(implementation = PaymentStartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid orderId"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not own the Order"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Conflicting Order/payment state")
    })
    public ResponseEntity<PaymentStartResponse> startPayment(@PathVariable("orderId") String orderId) {
        String userId = requireAuthenticatedUserId();
        return ResponseEntity.ok(orderPaymentMapper.toResponse(
                startPaymentUseCase.execute(new StartPaymentCommand(userId, orderId))));
    }

    private String requireAuthenticatedUserId() {
        return securityContextProvider.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication is required."));
    }
}
