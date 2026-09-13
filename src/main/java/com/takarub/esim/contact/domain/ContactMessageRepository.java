package com.takarub.esim.contact.domain;

import java.util.List;
import java.util.Optional;

public interface ContactMessageRepository {

    ContactMessage save(ContactMessage message);

    Optional<ContactMessage> findById(ContactMessageId id);

    List<ContactMessage> findAllNewestFirst();
}
