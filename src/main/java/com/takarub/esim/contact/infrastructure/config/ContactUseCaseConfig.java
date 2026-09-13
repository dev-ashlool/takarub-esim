package com.takarub.esim.contact.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.contact.application.usecase.ChangeContactStatusUseCase;
import com.takarub.esim.contact.application.usecase.ListContactMessagesUseCase;
import com.takarub.esim.contact.application.usecase.SubmitContactMessageUseCase;
import com.takarub.esim.contact.domain.ContactMessageRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

@Configuration
public class ContactUseCaseConfig {

    @Bean
    public SubmitContactMessageUseCase submitContactMessageUseCase(
            TransactionRunner transactionRunner,
            ContactMessageRepository contactMessageRepository,
            IdGenerator idGenerator,
            ClockProvider clockProvider) {
        return new SubmitContactMessageUseCase(
                transactionRunner, contactMessageRepository, idGenerator, clockProvider);
    }

    @Bean
    public ListContactMessagesUseCase listContactMessagesUseCase(
            ContactMessageRepository contactMessageRepository) {
        return new ListContactMessagesUseCase(contactMessageRepository);
    }

    @Bean
    public ChangeContactStatusUseCase changeContactStatusUseCase(
            TransactionRunner transactionRunner,
            ContactMessageRepository contactMessageRepository,
            ClockProvider clockProvider) {
        return new ChangeContactStatusUseCase(
                transactionRunner, contactMessageRepository, clockProvider);
    }
}
