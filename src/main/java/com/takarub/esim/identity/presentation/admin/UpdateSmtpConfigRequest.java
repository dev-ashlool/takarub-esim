package com.takarub.esim.identity.presentation.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSmtpConfigRequest(
        @NotBlank String host,
        @NotNull Integer port,
        @NotBlank String username,
        String password,
        Boolean authEnabled,
        Boolean tlsEnabled,
        @NotBlank @Email String fromEmail) {
}
