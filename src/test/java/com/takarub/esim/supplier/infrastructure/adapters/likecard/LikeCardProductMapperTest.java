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
                "5653",
                "JO",
                null,
                "Jordan 20GB",
                "9.99",
                null,
                "دولار",
                null,
                "20",
                "GB",
                "30",
                null);

        RawSupplierProduct product = mapper.toDomain(dto);

        assertThat(product.id()).isEqualTo("5653");
        assertThat(product.countryIso()).isEqualTo("JO");
        assertThat(product.costPrice()).isEqualByComparingTo("9.99");
        assertThat(product.costCurrency()).isEqualTo("USD");
        assertThat(product.dataAmount()).isEqualTo(20);
        assertThat(product.dataUnit()).isEqualTo(DataUnit.GB);
        assertThat(product.durationDays()).isEqualTo(30);
    }
}
