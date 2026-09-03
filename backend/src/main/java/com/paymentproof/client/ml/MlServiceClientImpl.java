package com.paymentproof.client.ml;

import com.paymentproof.client.ml.dto.MlClassificationRequest;
import com.paymentproof.client.ml.dto.MlClassificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlServiceClientImpl implements MlServiceClient {

    private final WebClient mlWebClient;

    @Override
    public MlClassificationResponse classifyIncident(MlClassificationRequest request) {
        log.info("Dispatching ML inference request for transactionId: {}", request.getTransaction_id());
        try {
            return mlWebClient.post()
                    .uri("/api/v1/ml/classify-incident")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MlClassificationResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            log.error("ML Service call failed for tx: {}. Falling back to rule-based evaluation. Error: {}",
                    request.getTransaction_id(), e.getMessage());
            // Safe fallback so backend remains operational even if ML service is temporarily down
            return MlClassificationResponse.builder()
                    .transaction_id(request.getTransaction_id())
                    .predicted_classification("OFFLINE_HEURISTIC_EVALUATION")
                    .confidence_score(java.math.BigDecimal.valueOf(0.50))
                    .anomaly_score(java.math.BigDecimal.valueOf(0.50))
                    .root_cause_hypothesis("ML Service unreachable; automated rule fallback active.")
                    .recommended_action_hypothesis("MANUAL_BANK_ESCALATION")
                    .build();
        }
    }

    @Override
    public Map<String, Object> checkHealth() {
        try {
            return mlWebClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(3))
                    .block();
        } catch (Exception e) {
            return Map.of(
                    "status", "DOWN",
                    "error", e.getMessage() != null ? e.getMessage() : "Connection failed"
            );
        }
    }
}
