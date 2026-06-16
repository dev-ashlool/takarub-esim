package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Command to create a session for an already-identified, authentication-eligible user. Carries the
 * user identifier and the device metadata captured for the session.
 */
public record CreateSessionCommand(
        String userId,
        String deviceName,
        String deviceType,
        String ipAddress,
        String userAgent
) {

    public CreateSessionCommand {
        if (userId == null || userId.isBlank()) {
            throw new ValidationException("User id is required");
        }
        if (deviceName == null || deviceName.isBlank()) {
            throw new ValidationException("Device name is required");
        }
        if (deviceType == null || deviceType.isBlank()) {
            throw new ValidationException("Device type is required");
        }
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new ValidationException("IP address is required");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new ValidationException("User agent is required");
        }
    }
}
