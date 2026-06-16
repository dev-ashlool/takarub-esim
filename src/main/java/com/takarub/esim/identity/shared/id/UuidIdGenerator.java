package com.takarub.esim.identity.shared.id;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Default production {@link IdGenerator} using random (type 4) UUIDs.
 */
@Component
public class UuidIdGenerator implements IdGenerator {

    @Override
    public UUID newUuid() {
        return UUID.randomUUID();
    }

    @Override
    public String newId() {
        return newUuid().toString();
    }
}
