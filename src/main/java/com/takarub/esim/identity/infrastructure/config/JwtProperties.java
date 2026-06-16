package com.takarub.esim.identity.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration bound from the {@code identity.jwt.*} namespace.
 */
@ConfigurationProperties(prefix = "identity.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenExpiration
) {
}
