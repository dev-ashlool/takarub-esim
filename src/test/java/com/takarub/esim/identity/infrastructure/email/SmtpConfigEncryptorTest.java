package com.takarub.esim.identity.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmtpConfigEncryptorTest {

    private final SmtpConfigEncryptor encryptor =
            new SmtpConfigEncryptor("test-password", "ab12cd34ef567890");

    @Test
    void encryptAndDecryptRoundTrip() {
        String plainText = "my-smtp-password-123!";
        String encrypted = encryptor.encrypt(plainText);

        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(encryptor.decrypt(encrypted)).isEqualTo(plainText);
    }

    @Test
    void sameInputProducesDifferentCiphertext() {
        String plainText = "same-password";
        String encrypted1 = encryptor.encrypt(plainText);
        String encrypted2 = encryptor.encrypt(plainText);

        assertThat(encrypted1).isNotEqualTo(encrypted2);
        assertThat(encryptor.decrypt(encrypted1)).isEqualTo(plainText);
        assertThat(encryptor.decrypt(encrypted2)).isEqualTo(plainText);
    }

    @Test
    void handlesSpecialCharacters() {
        String plainText = "p@$$w0rd!#%^&*()_+{}|:<>?";
        String encrypted = encryptor.encrypt(plainText);

        assertThat(encryptor.decrypt(encrypted)).isEqualTo(plainText);
    }
}
