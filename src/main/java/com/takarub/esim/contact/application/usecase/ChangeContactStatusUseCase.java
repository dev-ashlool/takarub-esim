package com.takarub.esim.contact.application.usecase;

import com.takarub.esim.contact.application.result.ContactMessageView;
import com.takarub.esim.contact.domain.ContactMessage;
import com.takarub.esim.contact.domain.ContactMessageId;
import com.takarub.esim.contact.domain.ContactMessageRepository;
import com.takarub.esim.contact.domain.ContactStatus;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.exception.ResourceNotFoundException;
import com.takarub.esim.identity.shared.time.ClockProvider;

public class ChangeContactStatusUseCase {

    private final TransactionRunner transactionRunner;
    private final ContactMessageRepository contactMessageRepository;
    private final ClockProvider clock;

    public ChangeContactStatusUseCase(TransactionRunner transactionRunner,
                                      ContactMessageRepository contactMessageRepository,
                                      ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.contactMessageRepository = contactMessageRepository;
        this.clock = clock;
    }

    public ContactMessageView execute(String rawId, ContactStatus status) {
        return transactionRunner.execute(() -> {
            ContactMessageId id = ContactMessageId.of(rawId);
            ContactMessage message = contactMessageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Contact message not found"));
            message.changeStatus(status, clock);
            contactMessageRepository.save(message);
            return ContactMessageView.from(message);
        });
    }
}
