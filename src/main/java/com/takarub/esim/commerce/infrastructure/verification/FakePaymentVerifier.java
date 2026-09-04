package com.takarub.esim.commerce.infrastructure.verification;

import java.math.BigDecimal;

import com.takarub.esim.commerce.application.port.PaymentVerificationRequest;
import com.takarub.esim.commerce.application.port.PaymentVerifier;
import com.takarub.esim.commerce.application.port.VerifiedPaymentOutcome;
import com.takarub.esim.commerce.application.port.VerifiedPaymentResult;
import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Deterministic local/dev {@link PaymentVerifier}. Interprets opaque verification references only;
 * never accesses repositories or mutates domain aggregates. Not a production PSP adapter.
 */
public class FakePaymentVerifier implements PaymentVerifier {

    static final String REF_SUCCESS = "success";
    static final String REF_FAILURE = "failure";
    static final String REF_AMOUNT_MISMATCH = "amount-mismatch";
    static final String REF_CURRENCY_MISMATCH = "currency-mismatch";

    @Override
    public VerifiedPaymentResult verify(PaymentVerificationRequest request) {
        if (request == null) {
            throw new ValidationException("Payment verification request is required");
        }
        String reference = request.verificationInput();
        if (reference == null || reference.isBlank()) {
            throw new ValidationException("Verification reference is required");
        }

        String attemptId = request.paymentAttemptId().value().toString();
        String externalOrderId = "dev-order-" + attemptId;
        String externalTransactionId = "dev-tx-" + attemptId;

        return switch (reference) {
            case REF_SUCCESS -> new VerifiedPaymentResult(
                    VerifiedPaymentOutcome.SUCCEEDED,
                    request.expectedAmount(),
                    request.expectedCurrency(),
                    externalOrderId,
                    externalTransactionId);
            case REF_FAILURE -> new VerifiedPaymentResult(
                    VerifiedPaymentOutcome.FAILED,
                    request.expectedAmount(),
                    request.expectedCurrency(),
                    externalOrderId,
                    externalTransactionId);
            case REF_AMOUNT_MISMATCH -> new VerifiedPaymentResult(
                    VerifiedPaymentOutcome.SUCCEEDED,
                    deliberatelyMismatchedAmount(request.expectedAmount()),
                    request.expectedCurrency(),
                    null,
                    null);
            case REF_CURRENCY_MISMATCH -> new VerifiedPaymentResult(
                    VerifiedPaymentOutcome.SUCCEEDED,
                    request.expectedAmount(),
                    "XXX",
                    null,
                    null);
            default -> throw new ValidationException(
                    "Unknown verification reference: " + reference);
        };
    }

    private static BigDecimal deliberatelyMismatchedAmount(BigDecimal expected) {
        if (expected == null) {
            return new BigDecimal("0.001");
        }
        return expected.add(new BigDecimal("0.001"));
    }
}
