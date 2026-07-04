package com.takarub.esim.identity.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.identity.application.port.NotificationSender;
import com.takarub.esim.identity.infrastructure.email.SmtpConfigService;
import com.takarub.esim.identity.infrastructure.notification.LoggingNotificationSender;
import com.takarub.esim.identity.infrastructure.notification.SmtpNotificationSender;

@Configuration
public class NotificationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "identity.mail", name = "enabled", havingValue = "true")
    public NotificationSender smtpNotificationSender(SmtpConfigService smtpConfigService) {
        return new SmtpNotificationSender(smtpConfigService);
    }

    @Bean
    @ConditionalOnProperty(prefix = "identity.mail", name = "enabled", havingValue = "false",
            matchIfMissing = true)
    public NotificationSender loggingNotificationSender() {
        return new LoggingNotificationSender();
    }
}
