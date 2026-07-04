package com.takarub.esim.identity.infrastructure.email;

/**
 * Immutable snapshot of an SMTP configuration, always carrying the <em>decrypted</em> password.
 * Never persisted directly — the adapter layer encrypts the password before storage.
 */
public record SmtpConfig(
        Long id,
        String host,
        int port,
        String username,
        String password,
        boolean authEnabled,
        boolean tlsEnabled,
        String fromEmail,
        boolean active) {
}
