package com.takarub.esim.contact.application.usecase;

import java.util.List;

import com.takarub.esim.contact.application.result.ContactMessageView;
import com.takarub.esim.contact.domain.ContactMessageRepository;

public class ListContactMessagesUseCase {

    private final ContactMessageRepository contactMessageRepository;

    public ListContactMessagesUseCase(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    public List<ContactMessageView> execute() {
        return contactMessageRepository.findAllNewestFirst().stream()
                .map(ContactMessageView::from)
                .toList();
    }
}
