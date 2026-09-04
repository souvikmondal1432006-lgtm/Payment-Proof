package com.paymentproof.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentproof.dto.GeminiInvestigationResponseDto;
import com.paymentproof.dto.GeminiPromptPayloadDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GeminiInvestigationServiceTest {

    private ObjectMapper objectMapper;
    private GeminiInvestigationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Point to an unroutable local address to test network timeouts and connection refusal
        service = new GeminiInvestigationService(
                WebClient.builder(),
                objectMapper,
                "test-api-key",
                "gemini-2.5-flash",
                "http://127.0.0.1:59999",
                200
        );
    }

    private GeminiPromptPayloadDto createSamplePayload() {
        return GeminiPromptPayloadDto.builder()
                .paymentId("pay_test_001")
                .incidentId("inc_test_001")
                .orderId("ORD-999")
                .amount(BigDecimal.valueOf(2500.00))
                .currency("INR")
                .paymentMethod("UPI")
                .bankStatus("SUCCESS")
                .gatewayStatus("FAILED")
                .merchantOrderStatus("CANCELLED")
                .webhookStatus("DROPPED")
                .detectedContradictions(List.of("Ghost Debit detected"))
                .mlPredictedClass("BANK_DEBIT_GATEWAY_FAILURE")
                .mlConfidence(BigDecimal.valueOf(0.99))
                .isRetryProhibited(true)
                .retryProhibitionReason("Active bank debit confirmed")
                .moneyAtRisk(BigDecimal.valueOf(2500.00))
                .recommendedResolution("AUTO_REFUND_CUSTOMER")
                .isAutomaticActionAllowed(true)
                .build();
    }

    @Test
    @DisplayName("GEMINI ASSISTANT: Unconfigured API key returns empty without making network calls")
    void testUnconfiguredApiKeyReturnsEmpty() {
        GeminiInvestigationService unconfigured = new GeminiInvestigationService(
                WebClient.builder(),
                objectMapper,
                "", // empty API key
                "gemini-2.5-flash",
                "http://127.0.0.1:59999",
                200
        );

        assertFalse(unconfigured.isConfigured());
        Optional<GeminiInvestigationResponseDto> result = unconfigured.explainInvestigation(createSamplePayload());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("TEST 6: Gemini timeout / connection failure falls back gracefully without throwing")
    void testGeminiTimeoutFallsBackToDeterministicJavaInvestigation() {
        assertTrue(service.isConfigured());
        Optional<GeminiInvestigationResponseDto> result = service.explainInvestigation(createSamplePayload());
        assertTrue(result.isEmpty(), "Connection refusal or timeout must return empty Optional without throwing");
    }

    @Test
    @DisplayName("TEST 5: Gemini prompt strictly forbids inventing evidence or changing safety decisions")
    void testGeminiCannotCreateEvidenceNotSupplied() {
        GeminiPromptPayloadDto payload = createSamplePayload();
        String prompt = service.buildPrompt(payload);

        assertNotNull(prompt);
        assertTrue(prompt.contains("You are an advisory payment investigation explanation assistant."));
        assertTrue(prompt.contains("Java has already performed the authoritative payment investigation."));
        assertTrue(prompt.contains("You must explain the supplied investigation results."));
        assertTrue(prompt.contains("You must not independently determine the authoritative payment state."));
        assertTrue(prompt.contains("You must not make financial decisions."));
        assertTrue(prompt.contains("You must not recommend an action different from the Java safety decision."));
        assertTrue(prompt.contains("You must not invent missing evidence."));
        assertTrue(prompt.contains("You must not assume an event occurred when it is not present in the supplied evidence."));
        assertTrue(prompt.contains("The Java safety decision is authoritative."));
        assertTrue(prompt.contains("is_retry_prohibited: true"));
        assertTrue(prompt.contains("authoritative_recommended_resolution: AUTO_REFUND_CUSTOMER"));
        assertTrue(prompt.contains("pay_test_001"));
    }

    @Test
    @DisplayName("GEMINI ASSISTANT: Response parser cleanly unpacks candidates and strips markdown fences")
    void testParseGeminiResponse_CleanAndMarkdownStripping() {
        String innerJson = "{\n" +
                "  \"summary\": \"Customer charged at bank switch but gateway timed out.\",\n" +
                "  \"what_happened\": \"UPI debit succeeded but gateway dropped socket connection.\",\n" +
                "  \"evidence\": [\"Bank UTR confirmed debit\", \"Gateway logged timeout\"],\n" +
                "  \"contradictions\": [\"Bank SUCCESS vs Gateway FAILED\"],\n" +
                "  \"ml_reasoning\": \"Consistent with BANK_DEBIT_GATEWAY_FAILURE.\",\n" +
                "  \"uncertainty\": \"None.\",\n" +
                "  \"recommended_operator_action\": \"Execute AUTO_REFUND_CUSTOMER.\",\n" +
                "  \"customer_impact\": \"Funds deducted without ticket fulfillment.\",\n" +
                "  \"confidence_explanation\": \"99% confidence due to unambiguous UTR.\"\n" +
                "}";

        // Wrapped in markdown codeblock inside Gemini's candidate structure
        String rawGeminiResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\n" +
                "            \"text\": \"```json\\n" + innerJson.replace("\"", "\\\"").replace("\n", "\\n") + "\\n```\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Optional<GeminiInvestigationResponseDto> parsed = service.parseGeminiResponse(rawGeminiResponse, "pay_test_001");

        assertTrue(parsed.isPresent());
        GeminiInvestigationResponseDto dto = parsed.get();
        assertEquals("Customer charged at bank switch but gateway timed out.", dto.getSummary());
        assertEquals("UPI debit succeeded but gateway dropped socket connection.", dto.getWhatHappened());
        assertEquals(2, dto.getEvidence().size());
        assertEquals("Execute AUTO_REFUND_CUSTOMER.", dto.getRecommendedOperatorAction());
        assertEquals("Funds deducted without ticket fulfillment.", dto.getCustomerImpact());
    }

    @Test
    @DisplayName("TEST 7: Malformed Gemini response falls back safely without throwing")
    void testMalformedGeminiResponseFallsBackSafely() {
        // Missing candidates
        assertTrue(service.parseGeminiResponse("{}", "pay_01").isEmpty());

        // Empty candidates array
        assertTrue(service.parseGeminiResponse("{\"candidates\": []}", "pay_01").isEmpty());

        // Missing parts
        assertTrue(service.parseGeminiResponse("{\"candidates\": [{\"content\": {\"parts\": []}}]}", "pay_01").isEmpty());

        // Empty text
        assertTrue(service.parseGeminiResponse("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"\"}]}}]}", "pay_01").isEmpty());

        // Invalid JSON inside text
        assertTrue(service.parseGeminiResponse("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"not json\"}]}}]}", "pay_01").isEmpty());

        // Missing required field 'what_happened'
        assertTrue(service.parseGeminiResponse("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"{\\\"summary\\\": \\\"hi\\\"}\"}]}}]}", "pay_01").isEmpty());
    }
}
