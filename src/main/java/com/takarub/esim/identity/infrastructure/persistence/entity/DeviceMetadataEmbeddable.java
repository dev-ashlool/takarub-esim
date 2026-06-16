package com.takarub.esim.identity.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable persistence representation of the Session aggregate's device metadata value object.
 */
@Embeddable
public class DeviceMetadataEmbeddable {

    @Column(name = "device_name", length = 255, nullable = false)
    private String deviceName;

    @Column(name = "device_type", length = 64, nullable = false)
    private String deviceType;

    @Column(name = "ip_address", length = 45, nullable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 512, nullable = false)
    private String userAgent;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    protected DeviceMetadataEmbeddable() {
        // Required by JPA.
    }

    public DeviceMetadataEmbeddable(String deviceName, String deviceType, String ipAddress,
                                    String userAgent, Instant lastActivityAt) {
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.lastActivityAt = lastActivityAt;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }
}
