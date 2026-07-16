package com.takarub.esim.identity.infrastructure.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * Encrypts and decrypts SMTP passwords using AES-256. The encryption key is derived from
 * a configurable password + hex-encoded salt, both sourced from environment variables or
 * application properties.
 */
@Component
public class SmtpConfigEncryptor {

    private final TextEncryptor encryptor;

    public SmtpConfigEncryptor(
            @Value("${identity.smtp.encryption-password:default-dev-key-change-in-production}") String password,
            @Value("${identity.smtp.encryption-salt:ab12cd34ef567890}") String salt) {
        this.encryptor = Encryptors.text(password, salt);
    }

    public String encrypt(String plainText) {
        return encryptor.encrypt(plainText);
    }

    public String decrypt(String cipherText) {
        return encryptor.decrypt(cipherText);
    }
}
