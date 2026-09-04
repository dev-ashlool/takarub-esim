package com.takarub.esim.commerce.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.shared.exception.ValidationException;

class CheckoutRequestIdTest {

    @Test
    void acceptsValidUuidStyleValue() {
        String raw = "550e8400-e29b-41d4-a716-446655440000";
        assertThat(raw).hasSize(36);

        CheckoutRequestId id = CheckoutRequestId.of(raw);

        assertThat(id.value()).isEqualTo(raw);
    }

    @Test
    void preservesSurroundingWhitespaceWhenOverallValueIsNonBlankAndWithinMaxLength() {
        String raw = "  key-with-spaces  ";
        assertThat(raw).hasSizeLessThanOrEqualTo(36);
        assertThat(raw.isBlank()).isFalse();

        CheckoutRequestId id = CheckoutRequestId.of(raw);

        assertThat(id.value()).isEqualTo(raw);
        assertThat(id.value()).startsWith("  ").endsWith("  ");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> CheckoutRequestId.of(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("required");
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> CheckoutRequestId.of("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("required");
    }

    @Test
    void rejectsTooLong() {
        String tooLong = "a".repeat(37);
        assertThatThrownBy(() -> CheckoutRequestId.of(tooLong))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("36");
    }

    @Test
    void rejectsTooLongIncludingWhitespace() {
        String tooLong = " " + "a".repeat(36);
        assertThat(tooLong).hasSize(37);
        assertThatThrownBy(() -> CheckoutRequestId.of(tooLong))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("36");
    }
}
