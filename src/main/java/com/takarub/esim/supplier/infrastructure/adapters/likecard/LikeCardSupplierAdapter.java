package com.takarub.esim.supplier.infrastructure.adapters.likecard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;
import com.takarub.esim.supplier.domain.model.CountryInfo;
import com.takarub.esim.supplier.domain.model.RawSupplierProduct;
import com.takarub.esim.supplier.domain.model.SupplierType;
import com.takarub.esim.supplier.domain.port.SupplierCatalogClient;
import com.takarub.esim.supplier.infrastructure.dto.likecard.LikeCardCategoriesResponse;
import com.takarub.esim.supplier.infrastructure.dto.likecard.LikeCardCategoryData;
import com.takarub.esim.supplier.infrastructure.dto.likecard.LikeCardCountriesResponse;
import com.takarub.esim.supplier.infrastructure.dto.likecard.LikeCardCountryData;
import com.takarub.esim.supplier.infrastructure.dto.likecard.LikeCardProductResponse;

/**
 * LikeCard YaHala outbound adapter implementing {@link SupplierCatalogClient}.
 */
@Component
public class LikeCardSupplierAdapter implements SupplierCatalogClient {

    static final String CATEGORIES_PATH = "/online/yahala/categories";
    static final String COUNTRIES_PATH = "/online/yahala/countries";
    static final String PRODUCTS_PATH = "/online/yahala/products";

    private final RestClient.Builder restClientBuilder;
    private final LikeCardProductMapper productMapper;

    public LikeCardSupplierAdapter(RestClient.Builder restClientBuilder, LikeCardProductMapper productMapper) {
        this.restClientBuilder = restClientBuilder;
        this.productMapper = productMapper;
    }

    @Override
    public List<RawSupplierProduct> fetchRemoteCatalog(Map<String, String> credentials) {
        Map<String, RawSupplierProduct> deduplicated = new LinkedHashMap<>();

        for (String categoryId : fetchCategoryIds(credentials)) {
            for (CountryInfo country : fetchCountries(credentials, categoryId)) {
                try {
                    for (RawSupplierProduct product : fetchProducts(credentials, categoryId, country.iso())) {
                        deduplicated.put(product.id(), product);
                    }
                } catch (SupplierApiException ex) {
                    // Partial failure: skip this country and continue harvesting others.
                }
            }
        }

        return new ArrayList<>(deduplicated.values());
    }

    /**
     * Returns all category identifiers returned by the LikeCard categories API.
     */
    public List<String> fetchCategoryIds(Map<String, String> credentials) {
        LikeCardCategoriesResponse response = invokeCategoriesApi(credentials);
        if (response.data() == null || response.data().isEmpty()) {
            return List.of();
        }

        List<String> categoryIds = new ArrayList<>();
        for (LikeCardCategoryData category : response.data()) {
            String categoryId = extractCategoryId(category);
            if (categoryId != null) {
                categoryIds.add(categoryId);
            }
        }
        return List.copyOf(categoryIds);
    }

    /**
     * Returns all countries with metadata (ISO, name, flag image) for the given category.
     */
    public List<CountryInfo> fetchCountries(Map<String, String> credentials, String categoryId) {
        LikeCardCountriesResponse response = invokeCountriesApi(credentials, categoryId);
        if (response.data() == null || response.data().isEmpty()) {
            return List.of();
        }

        List<CountryInfo> countries = new ArrayList<>();
        for (LikeCardCountryData country : response.data()) {
            String countryIso = extractCountryIso(country);
            if (countryIso != null) {
                countries.add(new CountryInfo(countryIso, country.countryName(), country.countryImage()));
            }
        }
        return List.copyOf(countries);
    }

    /**
     * Fetches and maps products for a single category and country combination.
     */
    public List<RawSupplierProduct> fetchProducts(
            Map<String, String> credentials, String categoryId, String countryIso) {
        LikeCardProductResponse response = invokeProductsApi(credentials, categoryId, countryIso);
        if (response.data() == null || response.data().isEmpty()) {
            return List.of();
        }
        return response.data().stream()
                .map(productMapper::toDomain)
                .toList();
    }

    LikeCardCategoriesResponse invokeCategoriesApi(Map<String, String> credentials) {
        MultiValueMap<String, String> formData = baseAuthForm(credentials);
        return postForResponse(credentials, CATEGORIES_PATH, formData, LikeCardCategoriesResponse.class, "categories");
    }

    LikeCardCountriesResponse invokeCountriesApi(Map<String, String> credentials, String categoryId) {
        MultiValueMap<String, String> formData = baseAuthForm(credentials);
        formData.add("categoryId", requireNonBlank(categoryId, "categoryId"));
        return postForResponse(credentials, COUNTRIES_PATH, formData, LikeCardCountriesResponse.class, "countries");
    }

    LikeCardProductResponse invokeProductsApi(
            Map<String, String> credentials, String categoryId, String countryIso) {
        MultiValueMap<String, String> formData = baseAuthForm(credentials);
        formData.add("categoryId", requireNonBlank(categoryId, "categoryId"));
        formData.add("countryIso", requireNonBlank(countryIso, "countryIso"));
        return postForResponse(credentials, PRODUCTS_PATH, formData, LikeCardProductResponse.class, "products");
    }

    @Override
    public SupplierType getSupplierType() {
        return SupplierType.LIKE_CARD;
    }

    private MultiValueMap<String, String> baseAuthForm(Map<String, String> credentials) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("email", requireCredential(credentials, "email"));
        formData.add("password", requireCredential(credentials, "password"));
        formData.add("securityCode", requireCredential(credentials, "securityCode"));
        formData.add("deviceId", requireCredential(credentials, "deviceId"));
        formData.add("langId", requireCredential(credentials, "langId"));
        return formData;
    }

    private <T> T postForResponse(
            Map<String, String> credentials,
            String path,
            MultiValueMap<String, String> formData,
            Class<T> responseType,
            String operation) {
        RestClient restClient = restClientBuilder
                .baseUrl(trimTrailingSlash(requireCredential(credentials, "base_url")))
                .build();

        try {
            T response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formData)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new SupplierApiException("LikeCard " + operation + " response body was empty");
            }
            validateLogicFlag(response, operation);
            return response;
        } catch (RestClientException ex) {
            throw new SupplierApiException("LikeCard " + operation + " API call failed", ex);
        } catch (IllegalArgumentException ex) {
            throw new SupplierApiException(
                    "LikeCard " + operation + " payload mapping failed: " + ex.getMessage(), ex);
        }
    }

    private static void validateLogicFlag(Object response, String operation) {
        int responseFlag = switch (response) {
            case LikeCardCategoriesResponse categories -> categories.response();
            case LikeCardCountriesResponse countries -> countries.response();
            case LikeCardProductResponse products -> products.response();
            default -> throw new IllegalStateException("Unsupported LikeCard response type");
        };

        if (responseFlag == 0) {
            throw new SupplierApiException(
                    "LikeCard " + operation + " API returned logic error flag (response=0)");
        }
    }

    static String extractCategoryId(LikeCardCategoryData category) {
        if (category == null) {
            return null;
        }
        return firstNonBlank(category.categoryId(), category.id());
    }

    static String extractCountryIso(LikeCardCountryData country) {
        if (country == null) {
            return null;
        }
        return firstNonBlank(country.countryIso(), country.countryCode());
    }

    static String requireCredential(Map<String, String> credentials, String key) {
        if (credentials == null) {
            throw new SupplierApiException("Supplier credentials map must not be null");
        }
        String value = credentials.get(key);
        if (value == null || value.isBlank()) {
            throw new SupplierApiException("Missing required LikeCard credential: " + key);
        }
        return value;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback == null || fallback.isBlank()) {
            return null;
        }
        return fallback.trim();
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
