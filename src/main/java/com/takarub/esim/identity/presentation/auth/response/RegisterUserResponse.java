package com.takarub.esim.identity.presentation.auth.response;

public record RegisterUserResponse(
        String userId,
        String email,
        String status
) {
}
