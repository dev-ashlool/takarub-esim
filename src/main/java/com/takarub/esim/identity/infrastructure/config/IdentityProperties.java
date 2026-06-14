package com.takarub.esim.identity.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized identity configuration, bound from the {@code identity.*} namespace and consumed when
 * wiring the use-case beans. Immutable (constructor binding).
 *
 * @param sessionTtl           lifetime of a session before it expires
 * @param emailVerificationTtl lifetime of an issued e-mail verification
 * @param passwordResetTtl     lifetime of an issued password-reset verification
 */
@ConfigurationProperties(prefix = "identity")
public record IdentityProperties(
        Duration sessionTtl,
        Duration emailVerificationTtl,
        Duration passwordResetTtl
) {
}
