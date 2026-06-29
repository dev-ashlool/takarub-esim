package com.takarub.esim.supplier.application.port;

import java.util.Map;

/**
 * Application port for loading supplier integration credentials from persistent storage.
 */
public interface SupplierCredentialsPort {

    Map<String, String> getCredentials(String supplierKey);
}
