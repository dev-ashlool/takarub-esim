package com.takarub.esim.identity.presentation.customer.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerProfileRequest(
        @Size(max = 120) String fullName,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotBlank @Size(min = 8, max = 20) String phoneNumber
) {
}
