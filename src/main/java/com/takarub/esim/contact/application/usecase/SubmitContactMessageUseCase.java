package com.takarub.esim.contact.application.usecase;

import com.takarub.esim.contact.application.result.ContactMessageView;
import com.takarub.esim.contact.domain.ContactMessage;
import com.takarub.esim.contact.domain.ContactMessageRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

public class SubmitContactMessageUseCase {

    private final TransactionRunner transactionRunner;
    private final ContactMessageRepository contactMessageRepository;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public SubmitContactMessageUseCase(TransactionRunner transactionRunner,
                                       ContactMessageRepository contactMessageRepository,
                                       IdGenerator idGenerator,
                                       ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.contactMessageRepository = contactMessageRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public ContactMessageView execute(String name, String email, String phoneNumber, String message) {
        return transactionRunner.execute(() -> ContactMessageView.from(contactMessageRepository.save(
                ContactMessage.submit(idGenerator, clock, name, email, phoneNumber, message))));
    }
}
