package com.takarub.esim.contact.domain;

import java.util.Objects;
import java.util.UUID;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

public record ContactMessageId(UUID value) {

    public ContactMessageId {
        if (value == null) {
            throw new ValidationException("Contact message id is required");
        }
    }

    public static ContactMessageId of(String raw) {
        try {
            return new ContactMessageId(UUID.fromString(raw));
        } catch (RuntimeException ex) {
            throw new ValidationException("Contact message id is invalid");
        }
    }

    public static ContactMessageId generate(IdGenerator idGenerator) {
        return new ContactMessageId(idGenerator.newUuid());
    }
}
