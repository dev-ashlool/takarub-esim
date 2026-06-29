package com.takarub.esim.supplier.infrastructure.adapters.likecard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;
import com.takarub.esim.supplier.domain.model.RawSupplierProduct;
import com.takarub.esim.supplier.domain.model.SupplierType;

class LikeCardSupplierAdapterTest {

    private static final String BASE_URL = "https://api.likecard.example";

    private MockRestServiceServer mockServer;
    private LikeCardSupplierAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        adapter = new LikeCardSupplierAdapter(builder, new LikeCardProductMapper());
    }

    @Test
    void fetchCategoryIdsReturnsIdentifiersFromCategoriesApi() {
        expectCategories("""
                {
                  "response": 1,
                  "data": [
                    { "categoryId": "10", "categoryName": "Asia" },
                    { "id": "20", "categoryName": "Europe" }
                  ]
                }
                """);

        assertThat(adapter.fetchCategoryIds(credentials())).containsExactly("10", "20");
        mockServer.verify();
    }

    @Test
    void fetchCountryIsosReturnsIdentifiersFromCountriesApi() {
        expectCountries("10", """
                {
                  "response": 1,
                  "data": [
                    { "countryIso": "JO", "countryName": "Jordan" },
                    { "countryCode": "AE", "countryName": "UAE" }
                  ]
                }
                """);

        assertThat(adapter.fetchCountryIsos(credentials(), "10")).containsExactly("JO", "AE");
        mockServer.verify();
    }

    @Test
    void fetchProductsUsesCategoryAndCountryMultipartFields() {
        expectProducts("10", "JO", """
                {
                  "response": 1,
                  "data": [
                    {
                      "productId": "5653",
                      "countryCode": "JO",
                      "priceWithVat": "12.50",
                      "currency": "ريال",
                      "productData": "5",
                      "productDataUnit": "GB",
                      "validity": "7"
                    }
                  ]
                }
                """);

        List<RawSupplierProduct> products = adapter.fetchProducts(credentials(), "10", "JO");

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().id()).isEqualTo("5653");
        assertThat(products.getFirst().costCurrency()).isEqualTo("SAR");
        mockServer.verify();
    }

    @Test
    void fetchRemoteCatalogHarvestsProductsAcrossCategoriesAndCountries() {
        expectCategories("""
                {
                  "response": 1,
                  "data": [
                    { "categoryId": "10", "categoryName": "Regional" },
                    { "categoryId": "20", "categoryName": "Global" }
                  ]
                }
                """);
        expectCountries("10", """
                {
                  "response": 1,
                  "data": [
                    { "countryIso": "JO" },
                    { "countryIso": "AE" }
                  ]
                }
                """);
        expectProducts("10", "JO", productPayload("1001", "JO", "9.99"));
        expectProducts("10", "AE", emptyProductsPayload());
        expectCountries("20", """
                {
                  "response": 1,
                  "data": [
                    { "countryIso": "SA" }
                  ]
                }
                """);
        expectProducts("20", "SA", productPayload("2001", "SA", "14.50"));

        List<RawSupplierProduct> products = adapter.fetchRemoteCatalog(credentials());

        assertThat(products).hasSize(2);
        assertThat(products).extracting(RawSupplierProduct::id).containsExactlyInAnyOrder("1001", "2001");
        assertThat(adapter.getSupplierType()).isEqualTo(SupplierType.LIKE_CARD);
        mockServer.verify();
    }

    @Test
    void fetchRemoteCatalogReturnsEmptyListWhenCategoriesAreEmpty() {
        expectCategories("""
                {
                  "response": 1,
                  "data": []
                }
                """);

        assertThat(adapter.fetchRemoteCatalog(credentials())).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchRemoteCatalogSkipsCategoryWhenCountriesAreEmpty() {
        expectCategories("""
                {
                  "response": 1,
                  "data": [
                    { "categoryId": "10" }
                  ]
                }
                """);
        expectCountries("10", """
                {
                  "response": 1,
                  "data": []
                }
                """);

        assertThat(adapter.fetchRemoteCatalog(credentials())).isEmpty();
        mockServer.verify();
    }

    @Test
    void fetchRemoteCatalogReturnsEmptyListWhenProductsAreEmpty() {
        expectCategories("""
                {
                  "response": 1,
                  "data": [
                    { "categoryId": "10" }
                  ]
                }
                """);
        expectCountries("10", """
                {
                  "response": 1,
                  "data": [
                    { "countryIso": "JO" }
                  ]
                }
                """);
        expectProducts("10", "JO", emptyProductsPayload());

        assertThat(adapter.fetchRemoteCatalog(credentials())).isEmpty();
        mockServer.verify();
    }

    @Test
    void rejectsLogicErrorResponseFlagFromCategoriesApi() {
        expectCategories("{\"response\":0,\"data\":[]}");

        assertThatThrownBy(() -> adapter.fetchCategoryIds(credentials()))
                .isInstanceOf(SupplierApiException.class)
                .hasMessageContaining("categories");
    }

    @Test
    void fetchRemoteCatalogContinuesWhenOneCountryProductsCallFails() {
        expectCategories("""
                {
                  "response": 1,
                  "data": [
                    { "categoryId": "10" }
                  ]
                }
                """);
        expectCountries("10", """
                {
                  "response": 1,
                  "data": [
                    { "countryIso": "JO" },
                    { "countryIso": "AE" }
                  ]
                }
                """);
        mockServer.expect(requestTo(BASE_URL + "/online/yahala/products"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());
        expectProducts("10", "AE", productPayload("2002", "AE", "11.00"));

        List<RawSupplierProduct> products = adapter.fetchRemoteCatalog(credentials());

        assertThat(products).hasSize(1);
        assertThat(products.getFirst().id()).isEqualTo("2002");
        mockServer.verify();
    }

    private void expectCategories(String body) {
        mockServer.expect(requestTo(BASE_URL + "/online/yahala/categories"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectCountries(String categoryId, String body) {
        mockServer.expect(requestTo(BASE_URL + "/online/yahala/countries"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectProducts(String categoryId, String countryIso, String body) {
        mockServer.expect(requestTo(BASE_URL + "/online/yahala/products"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private static String productPayload(String productId, String countryCode, String price) {
        return """
                {
                  "response": 1,
                  "data": [
                    {
                      "productId": "%s",
                      "countryCode": "%s",
                      "priceWithVat": "%s",
                      "currency": "دولار",
                      "productData": "5",
                      "productDataUnit": "GB",
                      "validity": "7"
                    }
                  ]
                }
                """.formatted(productId, countryCode, price);
    }

    private static String emptyProductsPayload() {
        return """
                {
                  "response": 1,
                  "data": []
                }
                """;
    }

    private static Map<String, String> credentials() {
        return Map.of(
                "base_url", BASE_URL,
                "email", "merchant@likecard.example",
                "password", "secret",
                "securityCode", "123456",
                "deviceId", "device-001",
                "langId", "1");
    }
}
