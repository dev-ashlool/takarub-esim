package com.takarub.esim.contact.application.result;

import java.time.Instant;

import com.takarub.esim.contact.domain.ContactMessage;
import com.takarub.esim.contact.domain.ContactStatus;

public record ContactMessageView(
        String id,
        String name,
        String email,
        String phoneNumber,
        String message,
        ContactStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ContactMessageView from(ContactMessage message) {
        return new ContactMessageView(
                message.id().value().toString(),
                message.name(),
                message.email().value(),
                message.phoneNumber(),
                message.message(),
                message.status(),
                message.createdAt(),
                message.updatedAt());
    }
}
