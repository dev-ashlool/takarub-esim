package com.takarub.esim.supplier.infrastructure.adapters.likecard;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.supplier.application.port.SupplierCredentialsPort;
import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;
import com.takarub.esim.supplier.domain.port.SupplierPurchasePort;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseRequest;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseResult;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseSuccessPayload;
import com.takarub.esim.supplier.infrastructure.dto.likecard.LikeCardBuyResponse;

/**
 * LikeCard YaHala {@code POST /online/yahala/buy} purchase adapter. Gated by
 * {@code takarub.supplier.likecard-purchase.enabled}; never falls back to the fake adapter.
 */
@Component
@ConditionalOnProperty(
        prefix = "takarub.supplier.likecard-purchase",
        name = "enabled",
        havingValue = "true")
public class LikeCardPurchaseAdapter implements SupplierPurchasePort {

    static final String BUY_PATH = "/online/yahala/buy";

    static final String CODE_INVALID_SUCCESS = "LIKECARD_INVALID_SUCCESS_PAYLOAD";
    static final String CODE_REJECTED = "LIKECARD_PURCHASE_REJECTED";
    static final String CODE_IP_BLOCKED = "LIKECARD_IP_BLOCKED";
    static final String CODE_UNKNOWN = "LIKECARD_PURCHASE_OUTCOME_UNKNOWN";

    private static final String MSG_INVALID_SUCCESS =
            "LikeCard buy returned success without usable order or activation material";
    private static final String MSG_REJECTED_FALLBACK = "LikeCard buy rejected the purchase";
    private static final String MSG_IP_BLOCKED =
            "LikeCard blocked this IP; contact the LikeCard account manager";
    private static final String MSG_UNKNOWN =
            "LikeCard buy outcome is unknown after transport uncertainty";

    private final RestClient.Builder restClientBuilder;
    private final SupplierCredentialsPort credentialsPort;

    public LikeCardPurchaseAdapter(
            RestClient.Builder restClientBuilder, SupplierCredentialsPort credentialsPort) {
        this.restClientBuilder = restClientBuilder;
        this.credentialsPort = credentialsPort;
    }

    @Override
    public SupplierPurchaseResult purchase(SupplierPurchaseRequest request) {
        if (request == null) {
            throw new ValidationException("Supplier purchase request is required");
        }

        Map<String, String> credentials = credentialsPort.getCredentials(request.supplierKey());
        MultiValueMap<String, String> formData = baseAuthForm(credentials);
        formData.add("productId", request.remoteProductId());

        RestClient restClient = restClientBuilder
                .baseUrl(trimTrailingSlash(requireCredential(credentials, "base_url")))
                .build();

        try {
            return restClient
                    .post()
                    .uri(BUY_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formData)
                    .exchange((httpRequest, httpResponse) -> {
                        int status = httpResponse.getStatusCode().value();
                        SupplierPurchaseResult transportMapped = mapTransportStatus(status);
                        if (transportMapped != null) {
                            return transportMapped;
                        }
                        if (httpResponse.getStatusCode().isError()) {
                            throw new SupplierApiException(
                                    "LikeCard buy API returned HTTP " + status);
                        }
                        LikeCardBuyResponse body =
                                httpResponse.bodyTo(LikeCardBuyResponse.class);
                        return mapBody(body);
                    });
        } catch (ResourceAccessException ex) {
            if (isTransportUncertainty(ex)) {
                return SupplierPurchaseResult.unknown(CODE_UNKNOWN, MSG_UNKNOWN);
            }
            throw new SupplierApiException("LikeCard buy transport failure", ex);
        } catch (RestClientResponseException ex) {
            SupplierPurchaseResult transportMapped = mapTransportStatus(ex.getStatusCode().value());
            if (transportMapped != null) {
                return transportMapped;
            }
            throw new SupplierApiException("LikeCard buy API call failed", ex);
        } catch (RestClientException ex) {
            if (isTransportUncertainty(ex)) {
                return SupplierPurchaseResult.unknown(CODE_UNKNOWN, MSG_UNKNOWN);
            }
            throw new SupplierApiException("LikeCard buy API call failed", ex);
        }
    }

    /**
     * Maps only documented/frozen transport statuses. Returns {@code null} when the caller should
     * either parse a success body or treat other HTTP errors as {@link SupplierApiException}.
     * Status {@code 1020} is documented by LikeCard as blocked IP (non-standard HTTP code).
     */
    static SupplierPurchaseResult mapTransportStatus(int status) {
        if (status == 1020) {
            return SupplierPurchaseResult.failed(CODE_IP_BLOCKED, MSG_IP_BLOCKED);
        }
        if (status == 408) {
            return SupplierPurchaseResult.unknown(CODE_UNKNOWN, MSG_UNKNOWN);
        }
        return null;
    }

    private static SupplierPurchaseResult mapBody(LikeCardBuyResponse body) {
        if (body == null) {
            return SupplierPurchaseResult.failed(
                    CODE_INVALID_SUCCESS, MSG_INVALID_SUCCESS);
        }
        if (body.response() == 0) {
            String message = blankToNull(body.message());
            return SupplierPurchaseResult.failed(
                    CODE_REJECTED, message != null ? message : MSG_REJECTED_FALLBACK);
        }
        if (body.response() != 1) {
            return SupplierPurchaseResult.failed(CODE_REJECTED, MSG_REJECTED_FALLBACK);
        }

        String orderId = blankToNull(body.orderId());
        String qrString = blankToNull(body.qrString());
        String smdpAddress = blankToNull(body.smdpAddress());
        String activationCode = blankToNull(body.activationCode());
        boolean usable =
                qrString != null || (smdpAddress != null && activationCode != null);
        if (orderId == null || !usable) {
            return SupplierPurchaseResult.failed(CODE_INVALID_SUCCESS, MSG_INVALID_SUCCESS);
        }

        return SupplierPurchaseResult.succeeded(
                new SupplierPurchaseSuccessPayload(
                        orderId,
                        blankToNull(body.iccid()),
                        smdpAddress,
                        activationCode,
                        blankToNull(body.pin()),
                        blankToNull(body.puk()),
                        qrString));
    }

    private static MultiValueMap<String, String> baseAuthForm(Map<String, String> credentials) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("email", requireCredential(credentials, "email"));
        formData.add("password", requireCredential(credentials, "password"));
        formData.add("securityCode", requireCredential(credentials, "securityCode"));
        formData.add("deviceId", requireCredential(credentials, "deviceId"));
        formData.add("langId", requireCredential(credentials, "langId"));
        return formData;
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

    /**
     * True only when the purchase request may already have reached LikeCard and the outcome cannot
     * be known (read/socket timeout, connection reset, broken pipe). Pre-request failures such as
     * connection refused are false so callers throw {@link SupplierApiException}.
     */
    static boolean isTransportUncertainty(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof ConnectException) {
                return false;
            }
            if (cursor instanceof SocketTimeoutException) {
                return true;
            }
            String message = cursor.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("connection refused")) {
                    return false;
                }
                if (lower.contains("connection reset") || lower.contains("broken pipe")) {
                    return true;
                }
                if (lower.contains("read timed out") || lower.contains("socket timed out")) {
                    return true;
                }
            }
            if (cursor instanceof SocketException && message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("connection reset") || lower.contains("broken pipe")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
