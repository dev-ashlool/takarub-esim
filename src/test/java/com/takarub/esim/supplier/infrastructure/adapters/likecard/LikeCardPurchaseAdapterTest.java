package com.takarub.esim.supplier.infrastructure.adapters.likecard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.takarub.esim.supplier.application.port.SupplierCredentialsPort;
import com.takarub.esim.supplier.domain.exceptions.SupplierApiException;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseOutcome;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseRequest;
import com.takarub.esim.supplier.domain.port.SupplierPurchaseResult;

class LikeCardPurchaseAdapterTest {

    private static final String BASE_URL = "https://api.likecard.example";
    private static final String PRODUCT_ID = "5585";

    private MockRestServiceServer mockServer;
    private LikeCardPurchaseAdapter adapter;
    private AtomicReference<String> lastRequestBody;

    @BeforeEach
    void setUp() {
        lastRequestBody = new AtomicReference<>();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        SupplierCredentialsPort credentialsPort = key -> credentials();
        adapter = new LikeCardPurchaseAdapter(builder, credentialsPort);
    }

    @Test
    void postsBuyWithoutIccidAndSucceedsWithQr() {
        expectBuy("""
                {
                  "response": 1,
                  "orderId": "12319604",
                  "iccid": "8943108161001722333",
                  "smdp_address": "",
                  "activation_code": "",
                  "pin": "12",
                  "puk": "3456",
                  "qrString": "LPA:1$rsp.truphone.com$JQ-1RB5HM-ID8TL5"
                }
                """);

        SupplierPurchaseResult result = adapter.purchase(request());

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.SUCCEEDED);
        assertThat(result.successPayload().supplierOrderId()).isEqualTo("12319604");
        assertThat(result.successPayload().iccid()).isEqualTo("8943108161001722333");
        assertThat(result.successPayload().qrString())
                .isEqualTo("LPA:1$rsp.truphone.com$JQ-1RB5HM-ID8TL5");
        assertThat(result.successPayload().pin()).isEqualTo("12");
        assertThat(result.successPayload().puk()).isEqualTo("3456");
        assertMultipartAuthAndProductIdWithoutIccid();
        mockServer.verify();
    }

    @Test
    void succeedsWithSmdpAndActivationCode() {
        expectBuy("""
                {
                  "response": 1,
                  "orderId": "99",
                  "iccid": "8943",
                  "smdp_address": "rsp.truphone.com",
                  "activation_code": "JQ-1RB5HM-ID8TL5",
                  "pin": null,
                  "puk": null,
                  "qrString": ""
                }
                """);

        SupplierPurchaseResult result = adapter.purchase(request());

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.SUCCEEDED);
        assertThat(result.successPayload().supplierOrderId()).isEqualTo("99");
        assertThat(result.successPayload().smdpAddress()).isEqualTo("rsp.truphone.com");
        assertThat(result.successPayload().activationCode()).isEqualTo("JQ-1RB5HM-ID8TL5");
        assertThat(result.successPayload().qrString()).isNull();
        mockServer.verify();
    }

    @Test
    void iccidOnlySuccessPayloadIsInvalidFailed() {
        expectBuy("""
                {
                  "response": 1,
                  "orderId": "12319604",
                  "iccid": "8943108161001722333",
                  "smdp_address": "",
                  "activation_code": "",
                  "qrString": ""
                }
                """);

        SupplierPurchaseResult result = adapter.purchase(request());

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.FAILED);
        assertThat(result.errorCode()).isEqualTo(LikeCardPurchaseAdapter.CODE_INVALID_SUCCESS);
        mockServer.verify();
    }

    @Test
    void missingOrderIdIsInvalidFailed() {
        expectBuy("""
                {
                  "response": 1,
                  "orderId": "",
                  "qrString": "LPA:1$rsp.truphone.com$CODE"
                }
                """);

        SupplierPurchaseResult result = adapter.purchase(request());

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.FAILED);
        assertThat(result.errorCode()).isEqualTo(LikeCardPurchaseAdapter.CODE_INVALID_SUCCESS);
        mockServer.verify();
    }

    @Test
    void responseZeroUsesSupplierMessage() {
        expectBuy("""
                {
                  "response": 0,
                  "message": "Insufficient balance"
                }
                """);

        SupplierPurchaseResult result = adapter.purchase(request());

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.FAILED);
        assertThat(result.errorCode()).isEqualTo(LikeCardPurchaseAdapter.CODE_REJECTED);
        assertThat(result.errorMessage()).isEqualTo("Insufficient balance");
        mockServer.verify();
    }

    @Test
    void responseZeroBlankMessageUsesFallback() {
        expectBuy("""
                {
                  "response": 0,
                  "message": "   "
                }
                """);

        SupplierPurchaseResult result = adapter.purchase(request());

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.FAILED);
        assertThat(result.errorCode()).isEqualTo(LikeCardPurchaseAdapter.CODE_REJECTED);
        assertThat(result.errorMessage()).contains("rejected");
        mockServer.verify();
    }

    @Test
    void http408IsUnknown() {
        mockServer
                .expect(requestTo(BASE_URL + LikeCardPurchaseAdapter.BUY_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withRawStatus(408));

        SupplierPurchaseResult result = adapter.purchase(request());

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.UNKNOWN);
        assertThat(result.errorCode()).isEqualTo(LikeCardPurchaseAdapter.CODE_UNKNOWN);
        mockServer.verify();
    }

    @Test
    void http500IsNotUnknown() {
        mockServer
                .expect(requestTo(BASE_URL + LikeCardPurchaseAdapter.BUY_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.purchase(request()))
                .isInstanceOf(SupplierApiException.class)
                .hasMessageContaining("HTTP 500");
        mockServer.verify();
    }

    @Test
    void transportTimeoutIsUnknown() {
        mockServer
                .expect(requestTo(BASE_URL + LikeCardPurchaseAdapter.BUY_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new ResourceAccessException(
                            "Read timed out", new SocketTimeoutException("Read timed out"));
                });

        SupplierPurchaseResult result = adapter.purchase(request());

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.UNKNOWN);
        assertThat(result.errorCode()).isEqualTo(LikeCardPurchaseAdapter.CODE_UNKNOWN);
        mockServer.verify();
    }

    @Test
    void connectionRefusedIsNotUnknown() {
        mockServer
                .expect(requestTo(BASE_URL + LikeCardPurchaseAdapter.BUY_PATH))
                .andExpect(method(HttpMethod.POST))
                .andRespond(request -> {
                    throw new ResourceAccessException(
                            "I/O error", new ConnectException("Connection refused"));
                });

        assertThatThrownBy(() -> adapter.purchase(request()))
                .isInstanceOf(SupplierApiException.class)
                .hasMessageContaining("transport failure");
        mockServer.verify();
    }

    @Test
    void http1020MappingHelperIsIpBlocked() {
        // Limitation: MockRestServiceServer / Spring HttpStatusCode reject non-standard HTTP 1020,
        // so this asserts only the shared mapTransportStatus branch (no live LikeCard call, no
        // MockRest round-trip for status 1020).
        SupplierPurchaseResult result = LikeCardPurchaseAdapter.mapTransportStatus(1020);

        assertThat(result.outcome()).isEqualTo(SupplierPurchaseOutcome.FAILED);
        assertThat(result.errorCode()).isEqualTo(LikeCardPurchaseAdapter.CODE_IP_BLOCKED);
        assertThat(LikeCardPurchaseAdapter.mapTransportStatus(500)).isNull();
    }

    @Test
    void missingCredentialFailsLocallyNotUnknown() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build();
        LikeCardPurchaseAdapter local = new LikeCardPurchaseAdapter(
                builder,
                key -> Map.of(
                        "base_url", BASE_URL,
                        "email", "merchant@likecard.example",
                        "password", "secret",
                        "securityCode", "123456",
                        "deviceId", "device-001"));

        assertThatThrownBy(() -> local.purchase(request()))
                .isInstanceOf(SupplierApiException.class)
                .hasMessageContaining("langId");
    }

    private void expectBuy(String body) {
        mockServer
                .expect(requestTo(BASE_URL + LikeCardPurchaseAdapter.BUY_PATH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(request -> {
                    ClientHttpRequest clientRequest = request;
                    if (clientRequest instanceof MockClientHttpRequest mock) {
                        lastRequestBody.set(mock.getBodyAsString());
                    }
                })
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void assertMultipartAuthAndProductIdWithoutIccid() {
        String body = lastRequestBody.get();
        assertThat(body).isNotBlank();
        assertThat(body).contains("email");
        assertThat(body).contains("password");
        assertThat(body).contains("deviceId");
        assertThat(body).contains("securityCode");
        assertThat(body).contains("langId");
        assertThat(body).contains("productId");
        assertThat(body).contains(PRODUCT_ID);
        assertThat(body).doesNotContain("name=\"iccid\"");
    }

    private static SupplierPurchaseRequest request() {
        return new SupplierPurchaseRequest(
                "LIKE_CARD", PRODUCT_ID, UUID.randomUUID().toString());
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
