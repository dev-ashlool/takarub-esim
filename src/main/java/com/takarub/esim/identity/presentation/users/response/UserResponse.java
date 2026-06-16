package com.takarub.esim.identity.presentation.users.response;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        String id,
        String email,
        String status,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
