package com.paymentproof.client;

import com.paymentproof.dto.MlAssessmentDto;
import com.paymentproof.dto.MlFeatureRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MlServiceClientTest {

    private MlServiceClient mlServiceClient;

    @BeforeEach
    void setUp() {
        // Point to non-routable port to test graceful failure and timeouts
        WebClient.Builder builder = WebClient.builder();
        mlServiceClient = new MlServiceClient(builder, "http://127.0.0.1:59999", 500);
    }

    @Test
    @DisplayName("PHASE 4: ML Service Unavailable / Connection Refused returns Optional.empty() without throwing exception")
    void testClassifyTelemetry_ConnectionRefused_ReturnsEmpty() {
        MlFeatureRequestDto request = MlFeatureRequestDto.builder()
                .paymentId("pay_test_offline")
                .amount(BigDecimal.valueOf(1500.00))
                .bankStatus("SUCCESS")
                .gatewayStatus("FAILED")
                .build();

        // Must NOT throw exception to caller and must NOT invent fake result
        Optional<MlAssessmentDto> result = mlServiceClient.classifyTelemetry(request);

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Must return empty Optional when ML service is offline");
    }
}
