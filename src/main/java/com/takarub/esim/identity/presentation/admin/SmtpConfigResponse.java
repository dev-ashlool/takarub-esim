package com.takarub.esim.identity.presentation.admin;

public record SmtpConfigResponse(
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
