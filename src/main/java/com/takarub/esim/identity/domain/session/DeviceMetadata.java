package com.takarub.esim.identity.domain.session;

import java.time.Instant;

import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Device descriptor associated with a {@link Session}, including the last activity timestamp.
 * Immutable; {@link #withLastActivityAt(Instant)} produces an updated copy.
 *
 * @param deviceName     human-readable device name
 * @param deviceType     device type descriptor (no fixed taxonomy is mandated by the domain)
 * @param ipAddress      originating IP address
 * @param userAgent      client user agent
 * @param lastActivityAt timestamp of the last recorded activity
 */
public record DeviceMetadata(
        String deviceName,
        String deviceType,
        String ipAddress,
        String userAgent,
        Instant lastActivityAt
) {

    public DeviceMetadata {
        if (deviceName == null || deviceName.isBlank()) {
            throw new ValidationException("Device name must not be blank");
        }
        if (deviceType == null || deviceType.isBlank()) {
            throw new ValidationException("Device type must not be blank");
        }
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new ValidationException("IP address must not be blank");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new ValidationException("User agent must not be blank");
        }
        if (lastActivityAt == null) {
            throw new ValidationException("Last activity timestamp must not be null");
        }
    }

    public static DeviceMetadata of(String deviceName, String deviceType, String ipAddress,
                                    String userAgent, Instant lastActivityAt) {
        return new DeviceMetadata(deviceName, deviceType, ipAddress, userAgent, lastActivityAt);
    }

    public DeviceMetadata withLastActivityAt(Instant newLastActivityAt) {
        return new DeviceMetadata(deviceName, deviceType, ipAddress, userAgent, newLastActivityAt);
    }
}
