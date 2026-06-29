package com.takarub.esim.supplier.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.catalog.application.port.CatalogPackagePort;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.supplier.application.port.SupplierCredentialsPort;
import com.takarub.esim.supplier.application.port.SupplierLikeCardProductLogPort;
import com.takarub.esim.supplier.application.port.SupplierPackageMappingPort;
import com.takarub.esim.supplier.application.usecases.SyncSupplierCatalogUseCase;
import com.takarub.esim.supplier.domain.port.SupplierCatalogClient;
import com.takarub.esim.supplier.infrastructure.adapters.likecard.LikeCardSupplierAdapter;

/**
 * Explicit Spring wiring for supplier application use cases.
 */
@Configuration
public class SupplierUseCaseConfig {

    @Bean
    public SyncSupplierCatalogUseCase syncSupplierCatalogUseCase(
            TransactionRunner transactionRunner,
            SupplierCredentialsPort credentialsPort,
            LikeCardSupplierAdapter likeCardSupplierAdapter,
            SupplierLikeCardProductLogPort likeCardProductLogPort,
            CatalogPackagePort catalogPackagePort,
            SupplierPackageMappingPort packageMappingPort) {
        return new SyncSupplierCatalogUseCase(
                transactionRunner,
                credentialsPort,
                likeCardSupplierAdapter,
                likeCardProductLogPort,
                catalogPackagePort,
                packageMappingPort);
    }

    @Bean
    public SupplierCatalogClient supplierCatalogClient(LikeCardSupplierAdapter likeCardSupplierAdapter) {
        return likeCardSupplierAdapter;
    }
}
