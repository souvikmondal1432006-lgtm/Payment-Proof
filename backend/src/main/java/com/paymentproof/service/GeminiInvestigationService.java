package com.paymentproof.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentproof.dto.GeminiInvestigationResponseDto;
import com.paymentproof.dto.GeminiPromptPayloadDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Gemini Explanation Assistant Service.
 * 
 * STRICT ARCHITECTURAL INVARIANT:
 * Gemini acts strictly as an explanation/investigation assistant.
 * It NEVER decides refunds, never overrides Java safety rules, never authorizes
 * financial actions, and never alters authoritative states.
 * If Gemini fails, times out, or is unconfigured, Java's deterministic
 * investigation result is used seamlessly without disruption.
 */
@Slf4j
@Service
public class GeminiInvestigationService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;

    public GeminiInvestigationService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${app.gemini.model:${GEMINI_MODEL:gemini-2.5-flash}}") String model,
            @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${app.gemini.timeout-ms:4000}") long timeoutMs) {

        this.objectMapper = objectMapper;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-2.5-flash";
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim() : "https://generativelanguage.googleapis.com/v1beta";
        this.timeout = Duration.ofMillis(timeoutMs > 0 ? timeoutMs : 4000);
        this.webClient = webClientBuilder
                .baseUrl(this.baseUrl)
                .build();
    }

    /**
     * Checks whether Gemini is configured with a non-blank API key.
     */
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /**
     * Sends structured investigation facts to Gemini for explanatory synthesis.
     * Returns Optional.empty() on missing key, timeout, rate limit, or any error.
     */
    public Optional<GeminiInvestigationResponseDto> explainInvestigation(GeminiPromptPayloadDto payload) {
        if (!isConfigured()) {
            log.info("Gemini API key is not configured (GEMINI_API_KEY is empty). Proceeding with Java deterministic report.");
            return Optional.empty();
        }

        try {
            log.info("Requesting Gemini investigation explanation for payment: {} using model: {}",
                    payload.getPaymentId(), model);

            String prompt = buildPrompt(payload);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    ),
                    "generationConfig", Map.of(
                            "response_mime_type", "application/json",
                            "temperature", 0.2
                    )
            );

            String responseBody = webClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Gemini API returned an empty response body for payment: {}", payload.getPaymentId());
                return Optional.empty();
            }

            return parseGeminiResponse(responseBody, payload.getPaymentId(), payload);

        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("Gemini API rate limit exceeded (HTTP 429) for payment: {}. Falling back to Java deterministic explanation.",
                    payload.getPaymentId());
            return Optional.empty();
        } catch (WebClientResponseException e) {
            log.warn("Gemini API returned HTTP {} ({}) for payment: {}. Falling back to Java deterministic explanation.",
                    e.getStatusCode(), e.getStatusText(), payload.getPaymentId());
            return Optional.empty();
        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException || e instanceof TimeoutException) {
                log.warn("Gemini API call timed out after {}ms for payment: {}. Falling back to Java deterministic explanation.",
                        timeout.toMillis(), payload.getPaymentId());
            } else {
                log.warn("Gemini investigation assistant failed for payment: {} ({}: {}). Falling back to Java deterministic explanation.",
                        payload.getPaymentId(), e.getClass().getSimpleName(), e.getMessage());
            }
            return Optional.empty();
        }
    }

    String buildPrompt(GeminiPromptPayloadDto payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an advisory payment investigation explanation assistant.\n\n");
        sb.append("Java has already performed the authoritative payment investigation.\n");
        sb.append("You must explain the supplied investigation results.\n");
        sb.append("You must not independently determine the authoritative payment state.\n");
        sb.append("You must not make financial decisions.\n");
        sb.append("You must not recommend an action different from the Java safety decision.\n");
        sb.append("You must not invent missing evidence.\n");
        sb.append("You must not assume an event occurred when it is not present in the supplied evidence.\n");
        sb.append("You must clearly distinguish:\n");
        sb.append("- confirmed facts\n");
        sb.append("- inferred explanation\n");
        sb.append("- uncertainty.\n");
        sb.append("If evidence is insufficient, state that evidence is insufficient.\n");
        sb.append("The Java safety decision is authoritative.\n\n");

        sb.append("JAVA SAFETY DECISION CONSTRAINTS (MANDATORY):\n");
        sb.append("- is_retry_prohibited: ").append(payload.isRetryProhibited()).append(" (").append(payload.getRetryProhibitionReason()).append(")\n");
        if (payload.isRetryProhibited()) {
            sb.append("  * CRITICAL: Because is_retry_prohibited is true, you must NEVER advise retrying or state 'Retry the payment'. You must explain why Java prohibited an automatic retry.\n");
        }
        sb.append("- is_automatic_action_allowed: ").append(payload.isAutomaticActionAllowed()).append("\n");
        if (!payload.isAutomaticActionAllowed()) {
            sb.append("  * CRITICAL: Because is_automatic_action_allowed is false, you must NEVER recommend an automatic action.\n");
        }
        sb.append("- authoritative_recommended_resolution: ").append(payload.getRecommendedResolution()).append("\n");
        sb.append("  * Your 'recommended_operator_action' field MUST describe and explain this Java-approved action (")
                .append(payload.getRecommendedResolution()).append("). It must NOT independently create a financial recommendation.\n\n");

        sb.append("SUPPLIED INVESTIGATION CONTEXT (ALREADY EVALUATED BY JAVA & RANDOM FOREST):\n");
        try {
            sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload)).append("\n\n");
        } catch (Exception e) {
            sb.append("Payment ID: ").append(payload.getPaymentId()).append(", Amount: ").append(payload.getAmount()).append("\n\n");
        }

        sb.append("REQUIRED RESPONSE FORMAT (STRICT JSON ONLY):\n");
        sb.append("{\n");
        sb.append("  \"summary\": \"Concise 1-2 sentence executive summary of the incident.\",\n");
        sb.append("  \"what_happened\": \"Clear, plain-English chronological narrative of what occurred across the banking switch, gateway, and merchant OMS based strictly on supplied evidence.\",\n");
        sb.append("  \"evidence\": [\"Confirmed telemetry fact 1\", \"Confirmed telemetry fact 2\"],\n");
        sb.append("  \"contradictions\": [\"Confirmed state divergence between systems\"],\n");
        sb.append("  \"ml_reasoning\": \"Explanation of why the Random Forest model predicted class (").append(payload.getMlPredictedClass())
                .append(") aligns with the domain signals.\",\n");
        sb.append("  \"uncertainty\": \"Unresolved questions or missing evidence requiring operator verification. If evidence is insufficient, state that evidence is insufficient.\",\n");
        sb.append("  \"recommended_operator_action\": \"Explanation of the Java-approved action (").append(payload.getRecommendedResolution())
                .append(") and why it is the safest next operational step according to Java safety rules.\",\n");
        sb.append("  \"customer_impact\": \"Concise assessment of how this incident impacts the customer's funds and checkout experience.\",\n");
        sb.append("  \"confidence_explanation\": \"Explanation of the statistical confidence score (").append(payload.getMlConfidence())
                .append(") based on signal clarity.\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    Optional<GeminiInvestigationResponseDto> parseGeminiResponse(String rawJson, String paymentId, GeminiPromptPayloadDto payload) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawJson);
            JsonNode candidates = rootNode.path("candidates");

            if (candidates.isMissingNode() || !candidates.isArray() || candidates.isEmpty()) {
                log.warn("Gemini response missing 'candidates' array for payment: {}", paymentId);
                return Optional.empty();
            }

            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");

            if (parts.isMissingNode() || !parts.isArray() || parts.isEmpty()) {
                log.warn("Gemini response missing content parts for payment: {}", paymentId);
                return Optional.empty();
            }

            String text = parts.get(0).path("text").asText("");
            if (text.isBlank()) {
                log.warn("Gemini candidate part text is empty for payment: {}", paymentId);
                return Optional.empty();
            }

            // Clean any potential markdown fences (e.g. ```json ... ```)
            String cleanJson = text.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            GeminiInvestigationResponseDto result = objectMapper.readValue(cleanJson, GeminiInvestigationResponseDto.class);

            // Validate that required fields exist and are meaningful
            if (result.getWhatHappened() == null || result.getWhatHappened().isBlank()) {
                log.warn("Gemini response missing required 'what_happened' field for payment: {}", paymentId);
                return Optional.empty();
            }

            // Populate metadata cleanly separating AI explanation from ML model
            result.setProvider("Google Gemini");
            result.setModelUsed(this.model);
            result.setExplainedAt(java.time.LocalDateTime.now());

            // Enforce Java Safety Decision on Gemini's operator action text
            if (payload != null && payload.isRetryProhibited()) {
                String actionText = result.getRecommendedOperatorAction();
                if (actionText != null && actionText.toLowerCase().contains("retry the payment")) {
                    log.warn("Gemini response contained prohibited retry recommendation for payment {}. Sanitizing to enforce Java safety rule.", paymentId);
                    result.setRecommendedOperatorAction(
                            String.format("Java Authoritative Action: %s. (Safety Invariant: Automatic/blind retry is strictly prohibited due to active funds at risk).",
                                    payload.getRecommendedResolution()));
                }
            }

            log.info("Successfully received and validated structured Gemini explanation for payment: {}", paymentId);
            return Optional.of(result);

        } catch (Exception e) {
            log.warn("Failed to parse Gemini JSON output for payment: {} ({}). Falling back to Java deterministic explanation.",
                    paymentId, e.getMessage());
            return Optional.empty();
        }
    }

    Optional<GeminiInvestigationResponseDto> parseGeminiResponse(String rawJson, String paymentId) {
        return parseGeminiResponse(rawJson, paymentId, null);
    }
}
