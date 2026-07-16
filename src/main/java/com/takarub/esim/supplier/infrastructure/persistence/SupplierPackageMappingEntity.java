package com.takarub.esim.supplier.infrastructure.persistence;

import java.math.BigDecimal;

import com.takarub.esim.catalog.infrastructure.persistence.CatalogPackageEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA persistence representation linking a catalog package to a supplier's remote product SKU.
 */
@Entity
@Table(name = "supplier_package_mappings")
public class SupplierPackageMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_package_id", nullable = false)
    private CatalogPackageEntity catalogPackage;

    @Column(name = "supplier_key", length = 50, nullable = false)
    private String supplierKey;

    @Column(name = "remote_product_id", length = 50, nullable = false)
    private String remoteProductId;

    @Column(name = "cost_price", precision = 12, scale = 4, nullable = false)
    private BigDecimal costPrice;

    @Column(name = "cost_currency", length = 3, nullable = false)
    private String costCurrency;

    @Column(name = "is_in_stock", nullable = false)
    private boolean inStock;

    protected SupplierPackageMappingEntity() {
        // Required by JPA.
    }

    public SupplierPackageMappingEntity(CatalogPackageEntity catalogPackage, String supplierKey,
                                        String remoteProductId, BigDecimal costPrice, String costCurrency,
                                        boolean inStock) {
        this.catalogPackage = catalogPackage;
        this.supplierKey = supplierKey;
        this.remoteProductId = remoteProductId;
        this.costPrice = costPrice;
        this.costCurrency = costCurrency;
        this.inStock = inStock;
    }

    public Integer getId() {
        return id;
    }

    public CatalogPackageEntity getCatalogPackage() {
        return catalogPackage;
    }

    public String getSupplierKey() {
        return supplierKey;
    }

    public String getRemoteProductId() {
        return remoteProductId;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public String getCostCurrency() {
        return costCurrency;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setCostCurrency(String costCurrency) {
        this.costCurrency = costCurrency;
    }
}
