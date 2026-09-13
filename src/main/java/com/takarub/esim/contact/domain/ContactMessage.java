package com.takarub.esim.contact.domain;

import java.time.Instant;

import com.takarub.esim.identity.domain.shared.AggregateRoot;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.PhoneNumber;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

public class ContactMessage extends AggregateRoot<ContactMessageId> {

    private final String name;
    private final EmailAddress email;
    private final String phoneNumber;
    private final String message;
    private ContactStatus status;

    private ContactMessage(ContactMessageId id, Instant createdAt, Instant updatedAt, String name,
                           EmailAddress email, String phoneNumber, String message, ContactStatus status) {
        super(id, createdAt, updatedAt);
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.status = status;
    }

    public static ContactMessage submit(IdGenerator idGenerator, ClockProvider clock, String name,
                                        String email, String phoneNumber, String message) {
        String normalizedName = requireName(name);
        String normalizedMessage = requireMessage(message);
        return new ContactMessage(
                ContactMessageId.generate(idGenerator),
                clock.now(),
                clock.now(),
                normalizedName,
                EmailAddress.of(email),
                PhoneNumber.normalizeOptional(phoneNumber),
                normalizedMessage,
                ContactStatus.NEW);
    }

    public static ContactMessage reconstitute(ContactMessageId id, Instant createdAt, Instant updatedAt,
                                              String name, EmailAddress email, String phoneNumber,
                                              String message, ContactStatus status) {
        return new ContactMessage(id, createdAt, updatedAt, name, email, phoneNumber, message, status);
    }

    public void changeStatus(ContactStatus newStatus, ClockProvider clock) {
        if (newStatus == null) {
            throw new ValidationException("Contact status is required");
        }
        this.status = newStatus;
        touch(clock);
    }

    public String name() {
        return name;
    }

    public EmailAddress email() {
        return email;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String message() {
        return message;
    }

    public ContactStatus status() {
        return status;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Name is required");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 120) {
            throw new ValidationException("Name must be at most 120 characters");
        }
        return trimmed;
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new ValidationException("Message is required");
        }
        String trimmed = message.trim();
        if (trimmed.length() > 2000) {
            throw new ValidationException("Message must be at most 2000 characters");
        }
        return trimmed;
    }
}
