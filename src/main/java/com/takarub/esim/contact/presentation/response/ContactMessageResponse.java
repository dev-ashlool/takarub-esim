package com.takarub.esim.contact.presentation.response;

import java.time.Instant;

import com.takarub.esim.contact.application.result.ContactMessageView;

public record ContactMessageResponse(
        String id,
        String name,
        String email,
        String phoneNumber,
        String message,
        String status,
        Instant createdAt,
        Instant updatedAt) {

    public static ContactMessageResponse from(ContactMessageView view) {
        return new ContactMessageResponse(
                view.id(),
                view.name(),
                view.email(),
                view.phoneNumber(),
                view.message(),
                view.status().name(),
                view.createdAt(),
                view.updatedAt());
    }
}
