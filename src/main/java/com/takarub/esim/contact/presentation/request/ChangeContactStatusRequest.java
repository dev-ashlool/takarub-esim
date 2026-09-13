package com.takarub.esim.contact.presentation.request;

import com.takarub.esim.contact.domain.ContactStatus;

import jakarta.validation.constraints.NotNull;

public record ChangeContactStatusRequest(@NotNull ContactStatus status) {
}
