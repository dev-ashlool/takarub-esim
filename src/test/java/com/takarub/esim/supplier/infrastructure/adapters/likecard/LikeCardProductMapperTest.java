package com.takarub.esim.supplier.infrastructure.adapters.likecard;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.RawSupplierProduct;
import com.takarub.esim.supplier.infrastructure.dto.likecard.LikeCardProductData;

class LikeCardProductMapperTest {

    private final LikeCardProductMapper mapper = new LikeCardProductMapper();

    @Test
    void mapsLikeCardDtoToRawSupplierProduct() {
        LikeCardProductData dto = new LikeCardProductData(
                "5653",       // id
                null,         // productId
                "JO",         // countryIso
                null,         // countryCode
                "Jordan 20GB",// productName
                "9.99",       // priceWithVat
                null,         // price
                "دولار",      // currency
                null,         // productCurrency
                "20",         // data
                null,         // productData
                "GB",         // dataUnit
                null,         // productDataUnit
                "30",         // duration
                null,         // validity
                null);        // validityDays

        RawSupplierProduct product = mapper.toDomain(dto);

        assertThat(product.id()).isEqualTo("5653");
        assertThat(product.countryIso()).isEqualTo("JO");
        assertThat(product.costPrice()).isEqualByComparingTo("9.99");
        assertThat(product.costCurrency()).isEqualTo("USD");
        assertThat(product.dataAmount()).isEqualTo(20);
        assertThat(product.dataUnit()).isEqualTo(DataUnit.GB);
        assertThat(product.durationDays()).isEqualTo(30);
    }

    @Test
    void mapsUnlimitedDataUnit() {
        LikeCardProductData dto = new LikeCardProductData(
                "10849", null, "US", null, "Unlimited Plan",
                "2.50", null, "دولار", null,
                "100", null, "% unlimited", null,
                "1", null, null);

        RawSupplierProduct product = mapper.toDomain(dto);

        assertThat(product.id()).isEqualTo("10849");
        assertThat(product.dataUnit()).isEqualTo(DataUnit.UNLIMITED);
        assertThat(product.costCurrency()).isEqualTo("USD");
    }
}
