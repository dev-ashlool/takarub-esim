package com.takarub.esim.commerce.domain.provisioning;

import java.util.UUID;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.IdGenerator;

/**
 * Strongly-typed identifier of a {@link ProvisionedEsim}. Immutable.
 */
public record ProvisionedEsimId(UUID value) {

    public ProvisionedEsimId {
        if (value == null) {
            throw new ValidationException("ProvisionedEsimId value must not be null");
        }
    }

    public static ProvisionedEsimId of(UUID value) {
        return new ProvisionedEsimId(value);
    }

    public static ProvisionedEsimId of(String value) {
        try {
            return new ProvisionedEsimId(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("ProvisionedEsimId must be a valid UUID");
        }
    }

    public static ProvisionedEsimId generate(IdGenerator idGenerator) {
        return new ProvisionedEsimId(idGenerator.newUuid());
    }
}
