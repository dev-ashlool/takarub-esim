package com.takarub.esim.supplier.infrastructure.persistence;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.takarub.esim.supplier.application.port.SupplierCredentialsPort;

/**
 * Application port adapter delegating to {@link SupplierCredentialsProvider}.
 */
@Component
public class SupplierCredentialsPortAdapter implements SupplierCredentialsPort {

    private final SupplierCredentialsProvider credentialsProvider;

    public SupplierCredentialsPortAdapter(SupplierCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public Map<String, String> getCredentials(String supplierKey) {
        return credentialsProvider.getCredentials(supplierKey);
    }
}
