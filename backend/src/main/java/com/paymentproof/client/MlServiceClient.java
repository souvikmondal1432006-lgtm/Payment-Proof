package com.paymentproof.client;

import com.paymentproof.dto.MlAssessmentDto;
import com.paymentproof.dto.MlFeatureRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class MlServiceClient {

    private static final Set<String> VALID_INCIDENT_CLASSES = Set.of(
            "NORMAL",
            "DELAYED_CONFIRMATION",
            "BANK_DEBIT_GATEWAY_FAILURE",
            "MISSING_WEBHOOK",
            "DUPLICATE_PAYMENT",
            "REFUND_UNCERTAINTY",
            "SETTLEMENT_MISMATCH",
            "ORDER_PAYMENT_CONFLICT",
            "UNRESOLVED"
    );

    private final WebClient webClient;
    private final String mlServiceUrl;
    private final Duration timeout;

    public MlServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.ml-service.url:http://localhost:8000}") String mlServiceUrl,
            @Value("${app.ml-service.timeout-ms:2500}") long timeoutMs) {
        this.mlServiceUrl = mlServiceUrl;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.webClient = webClientBuilder
                .baseUrl(mlServiceUrl)
                .build();
    }

    /**
     * Calls Python ML Service for incident classification.
     * Validates that the response is well-formed, confidence is strictly in [0.0, 1.0],
     * and the predicted class is recognized.
     * Returns Optional.empty() if unavailable, timed out, or malformed.
     * NEVER invents a fake ML response.
     */
    public Optional<MlAssessmentDto> classifyTelemetry(MlFeatureRequestDto featureRequest) {
        try {
            log.info("Requesting ML inference for payment: {} from {} with timeout {}ms",
                    featureRequest.getPaymentId(), mlServiceUrl, timeout.toMillis());

            MlAssessmentDto response = webClient.post()
                    .uri("/api/classify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(featureRequest)
                    .retrieve()
                    .bodyToMono(MlAssessmentDto.class)
                    .timeout(timeout)
                    .block();

            if (response == null) {
                log.warn("ML Service returned null body for payment: {}", featureRequest.getPaymentId());
                return Optional.empty();
            }

            // 1. Validate Predicted Class
            String predictedClass = response.getEffectivePredictedClass();
            if (predictedClass == null || !VALID_INCIDENT_CLASSES.contains(predictedClass.toUpperCase())) {
                log.warn("ML Service returned unknown or missing incident class: '{}' for payment: {}. Rejecting malformed response.",
                        predictedClass, featureRequest.getPaymentId());
                return Optional.empty();
            }

            // 2. Validate Confidence Score in range [0.0, 1.0]
            BigDecimal confidence = response.getEffectiveConfidence();
            if (confidence == null || confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
                log.warn("ML Service returned invalid confidence score: {} for payment: {}. Must be in [0.0, 1.0]. Rejecting malformed response.",
                        confidence, featureRequest.getPaymentId());
                return Optional.empty();
            }

            log.info("Received valid ML response: class={}, confidence={}", predictedClass, confidence);
            return Optional.of(response);

        } catch (Exception ex) {
            log.warn("External ML service unavailable or timed out at {} ({}). Returning empty assessment.",
                    mlServiceUrl, ex.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Checks if the external ML service is reachable and healthy.
     */
    public boolean isHealthy() {
        try {
            String status = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(1200))
                    .block();
            return status != null && status.contains("healthy");
        } catch (Exception e) {
            return false;
        }
    }
}
