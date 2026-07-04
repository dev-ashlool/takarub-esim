package com.takarub.esim.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMTP notification settings under {@code identity.mail.*}. Used when real e-mail delivery is
 * enabled; credentials themselves live in {@code spring.mail.*}.
 */
@ConfigurationProperties(prefix = "identity.mail")
public record MailProperties(
        boolean enabled,
        String from
) {
}
