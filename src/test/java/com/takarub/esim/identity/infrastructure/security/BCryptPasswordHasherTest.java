package com.takarub.esim.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.takarub.esim.identity.domain.user.PasswordHash;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void producesVerifiableBcryptHash() {
        PasswordHash hash = hasher.hash("s3cret-password");

        assertThat(hash).isNotNull();
        assertThat(hash.value()).isNotBlank();
        assertThat(hash.value()).startsWith("$2");
        assertThat(hash.value()).isNotEqualTo("s3cret-password");
        assertThat(new BCryptPasswordEncoder().matches("s3cret-password", hash.value())).isTrue();
    }

    @Test
    void usesRandomSaltSoHashesDiffer() {
        PasswordHash first = hasher.hash("same-password");
        PasswordHash second = hasher.hash("same-password");

        assertThat(first.value()).isNotEqualTo(second.value());
    }
}
