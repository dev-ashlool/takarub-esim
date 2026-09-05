package com.takarub.esim.commerce.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.shared.exception.ValidationException;

class CheckoutCommandTest {

    @Test
    void quantityOneIsAccepted() {
        CheckoutCommand command = new CheckoutCommand(
                UUID.randomUUID().toString(), "pkg-1", 1, UUID.randomUUID().toString());
        assertThat(command.quantity()).isEqualTo(1);
    }

    @Test
    void quantityZeroIsRejected() {
        assertThatThrownBy(() -> new CheckoutCommand(
                UUID.randomUUID().toString(), "pkg-1", 0, UUID.randomUUID().toString()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exactly 1");
    }

    @Test
    void quantityNegativeIsRejected() {
        assertThatThrownBy(() -> new CheckoutCommand(
                UUID.randomUUID().toString(), "pkg-1", -1, UUID.randomUUID().toString()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exactly 1");
    }

    @Test
    void quantityGreaterThanOneIsRejected() {
        assertThatThrownBy(() -> new CheckoutCommand(
                UUID.randomUUID().toString(), "pkg-1", 2, UUID.randomUUID().toString()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("exactly 1");
    }
}
