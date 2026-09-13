package com.takarub.esim.contact.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitContactRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email String email,
        @Size(max = 20) String phoneNumber,
        @NotBlank @Size(max = 2000) String message
) {
}
