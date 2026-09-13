package com.takarub.esim.identity.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.shared.exception.ValidationException;

class PhoneNumberTest {

    @Test
    void requireStripsSpaces() {
        assertThat(PhoneNumber.require(" +962 79 000 0000 ")).isEqualTo("+962790000000");
    }

    @Test
    void requireRejectsBlank() {
        assertThatThrownBy(() -> PhoneNumber.require(" "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectLetters() {
        assertThatThrownBy(() -> PhoneNumber.normalizeOptional("abc12345"))
                .isInstanceOf(ValidationException.class);
    }
}
