package com.takarub.esim.identity.presentation.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.takarub.esim.identity.infrastructure.email.SmtpConfig;
import com.takarub.esim.identity.infrastructure.email.SmtpConfigService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/email")
@Tag(name = "Email Administration", description = "Manage SMTP email configuration")
public class AdminEmailController {

    private final SmtpConfigService smtpConfigService;

    public AdminEmailController(SmtpConfigService smtpConfigService) {
        this.smtpConfigService = smtpConfigService;
    }

    @GetMapping("/smtp-config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get active SMTP configuration",
            description = "Returns the active SMTP configuration. The password field is always masked.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuration returned (or empty if none configured)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<SmtpConfigResponse> getSmtpConfig() {
        return smtpConfigService.getActiveConfig()
                .map(config -> ResponseEntity.ok(toResponse(config)))
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/smtp-config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create or update SMTP configuration",
            description = "Creates a new SMTP configuration or updates the existing one. "
                    + "Changes take effect immediately (cached sender is refreshed). "
                    + "Omit the password field to keep the existing password on updates.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuration saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<SmtpConfigResponse> updateSmtpConfig(
            @Valid @RequestBody UpdateSmtpConfigRequest request) {

        SmtpConfig config = new SmtpConfig(
                null,
                request.host(),
                request.port(),
                request.username(),
                request.password(),
                request.authEnabled() != null ? request.authEnabled() : true,
                request.tlsEnabled() != null ? request.tlsEnabled() : true,
                request.fromEmail(),
                true);

        SmtpConfig saved = smtpConfigService.saveConfig(config);
        return ResponseEntity.ok(toResponse(saved));
    }

    private SmtpConfigResponse toResponse(SmtpConfig config) {
        return new SmtpConfigResponse(
                config.id(),
                config.host(),
                config.port(),
                config.username(),
                config.password(),
                config.authEnabled(),
                config.tlsEnabled(),
                config.fromEmail(),
                config.active());
    }
}
