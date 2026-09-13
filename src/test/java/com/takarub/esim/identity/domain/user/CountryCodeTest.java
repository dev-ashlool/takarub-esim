package com.takarub.esim.identity.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.shared.exception.ValidationException;

class CountryCodeTest {

    @Test
    void requireNormalizesLowercaseIso2() {
        assertThat(CountryCode.require(" jo ")).isEqualTo("JO");
    }

    @Test
    void requireRejectsBlank() {
        assertThatThrownBy(() -> CountryCode.require(" "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectInvalidCode() {
        assertThatThrownBy(() -> CountryCode.normalizeOptional("JOR"))
                .isInstanceOf(ValidationException.class);
    }
}
