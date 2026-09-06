package com.takarub.esim.commerce.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.takarub.esim.commerce.application.port.CustomerEmailLookup;
import com.takarub.esim.commerce.application.port.EsimReadyCustomerNotifier;
import com.takarub.esim.commerce.infrastructure.notification.NoOpEsimReadyCustomerNotifier;
import com.takarub.esim.commerce.infrastructure.notification.SmtpEsimReadyCustomerNotifier;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.infrastructure.email.SmtpConfigService;

class CommerceEsimEmailNotificationConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CommerceEsimEmailNotificationConfig.class)
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(SmtpConfigService.class, () -> mock(SmtpConfigService.class));

    @Test
    void featureOffByDefaultWiresNoOpNotifier() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(CustomerEmailLookup.class);
            assertThat(context).hasSingleBean(EsimReadyCustomerNotifier.class);
            assertThat(context.getBean(EsimReadyCustomerNotifier.class))
                    .isInstanceOf(NoOpEsimReadyCustomerNotifier.class);
            assertThat(context).doesNotHaveBean(SmtpEsimReadyCustomerNotifier.class);
        });
    }

    @Test
    void featureOnWiresSmtpNotifier() {
        runner.withPropertyValues("takarub.commerce.esim-email-notification.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(EsimReadyCustomerNotifier.class);
                    assertThat(context.getBean(EsimReadyCustomerNotifier.class))
                            .isInstanceOf(SmtpEsimReadyCustomerNotifier.class);
                });
    }

    @Test
    void featureExplicitlyOffWiresNoOp() {
        runner.withPropertyValues("takarub.commerce.esim-email-notification.enabled=false")
                .run(context -> {
                    assertThat(context.getBean(EsimReadyCustomerNotifier.class))
                            .isInstanceOf(NoOpEsimReadyCustomerNotifier.class);
                });
    }
}
