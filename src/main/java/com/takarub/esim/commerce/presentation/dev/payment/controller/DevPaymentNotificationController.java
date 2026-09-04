package com.takarub.esim.commerce.presentation.dev.payment.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.commerce.application.usecase.HandlePaymentNotificationUseCase;
import com.takarub.esim.commerce.presentation.dev.payment.mapper.DevPaymentNotificationMapper;
import com.takarub.esim.commerce.presentation.dev.payment.request.DevPaymentNotificationRequest;
import com.takarub.esim.commerce.presentation.dev.payment.response.DevPaymentNotificationResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Local/dev-only payment notification trigger. Conditionally registered; not a production PSP webhook.
 */
@RestController
@RequestMapping("/api/v1/dev/payments")
@ConditionalOnProperty(
        prefix = "takarub.commerce.dev-payment-verification",
        name = "enabled",
        havingValue = "true")
@Tag(name = "Dev Payment Verification", description = "Local/dev payment notification trigger (disabled by default)")
public class DevPaymentNotificationController {

    private final HandlePaymentNotificationUseCase handlePaymentNotificationUseCase;
    private final DevPaymentNotificationMapper mapper;

    public DevPaymentNotificationController(
            HandlePaymentNotificationUseCase handlePaymentNotificationUseCase,
            DevPaymentNotificationMapper mapper) {
        this.handlePaymentNotificationUseCase = handlePaymentNotificationUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/notifications")
    @Operation(
            summary = "Trigger local/dev payment verification",
            description = "Accepts only paymentAttemptId and an opaque verificationReference. "
                    + "Trusted amount, currency, and outcome come from PaymentVerifier, not from this body. "
                    + "Absent unless takarub.commerce.dev-payment-verification.enabled=true.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Verification applied or same-result idempotent",
                    content = @Content(schema = @Schema(implementation = DevPaymentNotificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or verification reference"),
            @ApiResponse(responseCode = "404", description = "Payment attempt not found"),
            @ApiResponse(responseCode = "409", description = "Conflict / inconsistency / mismatch")
    })
    public ResponseEntity<DevPaymentNotificationResponse> notify(
            @Valid @RequestBody DevPaymentNotificationRequest request) {
        return ResponseEntity.ok(mapper.toResponse(
                handlePaymentNotificationUseCase.execute(mapper.toCommand(request))));
    }
}
