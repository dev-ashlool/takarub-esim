package com.takarub.esim.identity.presentation.auth.response;

public record VerifyEmailResponse(
        String userId,
        String status
) {
}
