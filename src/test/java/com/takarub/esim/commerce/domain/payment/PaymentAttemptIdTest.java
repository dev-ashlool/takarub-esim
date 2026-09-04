package com.takarub.esim.commerce.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;

class PaymentAttemptIdTest {

    @Test
    void ofUuidCreatesId() {
        UUID value = UUID.randomUUID();
        assertThat(PaymentAttemptId.of(value).value()).isEqualTo(value);
    }

    @Test
    void ofStringParsesUuid() {
        UUID value = UUID.randomUUID();
        assertThat(PaymentAttemptId.of(value.toString()).value()).isEqualTo(value);
    }

    @Test
    void nullUuidRejected() {
        assertThatThrownBy(() -> PaymentAttemptId.of((UUID) null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void invalidStringRejected() {
        assertThatThrownBy(() -> PaymentAttemptId.of("not-a-uuid"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void generateUsesIdGenerator() {
        PaymentAttemptId id = PaymentAttemptId.generate(new UuidIdGenerator());
        assertThat(id.value()).isNotNull();
    }
}
