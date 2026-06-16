package com.takarub.esim.identity.application.command;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Device metadata captured at login for session creation.
 */
public record AuthenticateUserDeviceMetadata(
        String deviceName,
        String deviceType,
        String ipAddress,
        String userAgent
) {

    public AuthenticateUserDeviceMetadata {
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
