package com.takarub.esim.commerce.infrastructure.verification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.commerce.application.port.PaymentVerificationRequest;
import com.takarub.esim.commerce.application.port.VerifiedPaymentOutcome;
import com.takarub.esim.commerce.application.port.VerifiedPaymentResult;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.identity.shared.exception.ValidationException;

class FakePaymentVerifierTest {

    private final FakePaymentVerifier verifier = new FakePaymentVerifier();

    @Test
    void successDerivesAmountCurrencyAndDeterministicExternalIds() {
        PaymentAttemptId attemptId = PaymentAttemptId.of(UUID.randomUUID());
        PaymentVerificationRequest request = request(attemptId, "9.99", "USD", "success");

        VerifiedPaymentResult result = verifier.verify(request);

        assertThat(result.outcome()).isEqualTo(VerifiedPaymentOutcome.SUCCEEDED);
        assertThat(result.verifiedAmount()).isEqualByComparingTo("9.99");
        assertThat(result.verifiedCurrency()).isEqualTo("USD");
        assertThat(result.externalOrderId()).isEqualTo("dev-order-" + attemptId.value());
        assertThat(result.externalTransactionId()).isEqualTo("dev-tx-" + attemptId.value());
    }

    @Test
    void failureDerivesAmountCurrencyFromRequest() {
        PaymentAttemptId attemptId = PaymentAttemptId.of(UUID.randomUUID());
        PaymentVerificationRequest request = request(attemptId, "9.99", "USD", "failure");

        VerifiedPaymentResult result = verifier.verify(request);

        assertThat(result.outcome()).isEqualTo(VerifiedPaymentOutcome.FAILED);
        assertThat(result.verifiedAmount()).isEqualByComparingTo("9.99");
        assertThat(result.verifiedCurrency()).isEqualTo("USD");
        assertThat(result.externalOrderId()).isEqualTo("dev-order-" + attemptId.value());
        assertThat(result.externalTransactionId()).isEqualTo("dev-tx-" + attemptId.value());
    }

    @Test
    void amountMismatchReturnsGenuinelyDifferentTrustedAmount() {
        PaymentVerificationRequest request =
                request(PaymentAttemptId.of(UUID.randomUUID()), "9.99", "USD", "amount-mismatch");

        VerifiedPaymentResult result = verifier.verify(request);

        assertThat(result.verifiedAmount().compareTo(new BigDecimal("9.99"))).isNotZero();
        assertThat(result.verifiedAmount()).isEqualByComparingTo("9.991");
        assertThat(result.verifiedCurrency()).isEqualTo("USD");
    }

    @Test
    void currencyMismatchReturnsGenuinelyDifferentTrustedCurrency() {
        PaymentVerificationRequest request =
                request(PaymentAttemptId.of(UUID.randomUUID()), "9.99", "USD", "currency-mismatch");

        VerifiedPaymentResult result = verifier.verify(request);

        assertThat(result.verifiedCurrency()).isNotEqualTo("USD");
        assertThat(result.verifiedAmount()).isEqualByComparingTo("9.99");
    }

    @Test
    void invalidReferenceThrowsValidationException() {
        PaymentVerificationRequest request =
                request(PaymentAttemptId.of(UUID.randomUUID()), "9.99", "USD", "not-a-valid-ref");

        assertThatThrownBy(() -> verifier.verify(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void fakeHasNoRepositoryConstructorDependencies() {
        Constructor<?>[] constructors = FakePaymentVerifier.class.getDeclaredConstructors();
        assertThat(constructors).hasSize(1);
        assertThat(constructors[0].getParameterCount()).isZero();
        assertThat(Arrays.stream(FakePaymentVerifier.class.getDeclaredFields())
                .noneMatch(field -> field.getType().getSimpleName().contains("Repository")))
                .isTrue();
    }

    @Test
    void sameRequestIsDeterministic() {
        PaymentAttemptId attemptId = PaymentAttemptId.of(UUID.randomUUID());
        PaymentVerificationRequest request = request(attemptId, "19.98", "USD", "success");

        VerifiedPaymentResult first = verifier.verify(request);
        VerifiedPaymentResult second = verifier.verify(request);

        assertThat(first).isEqualTo(second);
    }

    private static PaymentVerificationRequest request(PaymentAttemptId id,
                                                      String amount,
                                                      String currency,
                                                      String reference) {
        return new PaymentVerificationRequest(id, new BigDecimal(amount), currency, reference);
    }
}
