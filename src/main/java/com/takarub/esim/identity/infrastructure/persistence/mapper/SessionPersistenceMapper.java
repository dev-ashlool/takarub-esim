package com.takarub.esim.identity.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.domain.session.DeviceMetadata;
import com.takarub.esim.identity.domain.session.RefreshToken;
import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.infrastructure.persistence.entity.DeviceMetadataEmbeddable;
import com.takarub.esim.identity.infrastructure.persistence.entity.SessionJpaEntity;

/**
 * Translates between the {@link Session} aggregate and {@link SessionJpaEntity}. The reverse
 * direction uses the domain's explicit {@link Session#reconstitute} factory; no reflection is used.
 */
@Component
public class SessionPersistenceMapper {

    public SessionJpaEntity toEntity(Session session) {
        DeviceMetadata device = session.deviceMetadata();
        DeviceMetadataEmbeddable deviceEmbeddable = new DeviceMetadataEmbeddable(
                device.deviceName(),
                device.deviceType(),
                device.ipAddress(),
                device.userAgent(),
                device.lastActivityAt());
        return new SessionJpaEntity(
                session.id().value().toString(),
                session.userId().value().toString(),
                session.refreshToken().value(),
                session.status(),
                deviceEmbeddable,
                session.expiresAt(),
                session.createdAt(),
                session.updatedAt());
    }

    public Session toDomain(SessionJpaEntity entity) {
        DeviceMetadataEmbeddable deviceEmbeddable = entity.getDeviceMetadata();
        DeviceMetadata device = DeviceMetadata.of(
                deviceEmbeddable.getDeviceName(),
                deviceEmbeddable.getDeviceType(),
                deviceEmbeddable.getIpAddress(),
                deviceEmbeddable.getUserAgent(),
                deviceEmbeddable.getLastActivityAt());
        return Session.reconstitute(
                SessionId.of(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                UserId.of(entity.getUserId()),
                RefreshToken.of(entity.getRefreshToken()),
                entity.getStatus(),
                device,
                entity.getExpiresAt());
    }
}
