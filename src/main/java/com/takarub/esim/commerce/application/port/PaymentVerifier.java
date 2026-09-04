package com.takarub.esim.commerce.application.port;

/**
 * Provider-neutral verification of a payment attempt. Implementations may call a remote PSP or a
 * local fake; callers must invoke this outside any database transaction.
 */
public interface PaymentVerifier {

    VerifiedPaymentResult verify(PaymentVerificationRequest request);
}
