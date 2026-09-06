package com.takarub.esim.commerce.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.commerce.application.port.CustomerEmailLookup;
import com.takarub.esim.commerce.application.port.EsimReadyCustomerNotifier;
import com.takarub.esim.commerce.infrastructure.notification.NoOpEsimReadyCustomerNotifier;
import com.takarub.esim.commerce.infrastructure.notification.SmtpEsimReadyCustomerNotifier;
import com.takarub.esim.commerce.infrastructure.notification.UserRepositoryCustomerEmailLookup;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.infrastructure.email.SmtpConfigService;

/**
 * Wires best-effort eSIM-ready customer email notification. Default OFF (no-op notifier). Does not
 * alter identity {@code NotificationSender}.
 */
@Configuration
public class CommerceEsimEmailNotificationConfig {

    @Bean
    public CustomerEmailLookup customerEmailLookup(UserRepository userRepository) {
        return new UserRepositoryCustomerEmailLookup(userRepository);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "takarub.commerce.esim-email-notification",
            name = "enabled",
            havingValue = "true")
    public EsimReadyCustomerNotifier smtpEsimReadyCustomerNotifier(SmtpConfigService smtpConfigService) {
        return new SmtpEsimReadyCustomerNotifier(smtpConfigService);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "takarub.commerce.esim-email-notification",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true)
    public EsimReadyCustomerNotifier noOpEsimReadyCustomerNotifier() {
        return new NoOpEsimReadyCustomerNotifier();
    }
}
