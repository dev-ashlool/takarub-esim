package com.takarub.esim.identity.presentation.auth.response;

public record ResetPasswordResponse(
        String userId,
        int revokedSessionCount
) {
}
