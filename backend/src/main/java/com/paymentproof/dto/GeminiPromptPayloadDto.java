package com.paymentproof.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Structured investigation context sent to Gemini Explanation Assistant.
 * 
 * ARCHITECTURAL PRINCIPLE:
 * JAVA INVESTIGATES. ML CLASSIFIES. JAVA DECIDES. GEMINI EXPLAINS.
 * 
 * Gemini receives this fully evaluated context as established system facts.
 * Gemini must never override or mutate any of these authoritative values.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiPromptPayloadDto {

    // 1. PAYMENT FACTS
    @JsonProperty("payment_id")
    private String paymentId;

    @JsonProperty("incident_id")
    private String incidentId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("payment_method")
    private String paymentMethod;

    // 2. MULTI-PARTY EVIDENCE & TIMESTAMPS
    @JsonProperty("bank_name")
    private String bankName;

    @JsonProperty("bank_status")
    private String bankStatus;

    @JsonProperty("bank_utr")
    private String bankUtr;

    @JsonProperty("bank_latency_ms")
    private Integer bankLatencyMs;

    @JsonProperty("bank_timestamp")
    private String bankTimestamp;

    @JsonProperty("gateway_name")
    private String gatewayName;

    @JsonProperty("gateway_status")
    private String gatewayStatus;

    @JsonProperty("gateway_auth_status")
    private String gatewayAuthStatus;

    @JsonProperty("gateway_capture_status")
    private String gatewayCaptureStatus;

    @JsonProperty("gateway_latency_ms")
    private Integer gatewayLatencyMs;

    @JsonProperty("gateway_timestamp")
    private String gatewayTimestamp;

    @JsonProperty("merchant_order_status")
    private String merchantOrderStatus;

    @JsonProperty("merchant_fulfillment_status")
    private String merchantFulfillmentStatus;

    @JsonProperty("merchant_timestamp")
    private String merchantTimestamp;

    @JsonProperty("webhook_status")
    private String webhookStatus;

    @JsonProperty("webhook_http_code")
    private Integer webhookHttpCode;

    @JsonProperty("webhook_attempt_count")
    private Integer webhookAttemptCount;

    @JsonProperty("webhook_timestamp")
    private String webhookTimestamp;

    @JsonProperty("settlement_status")
    private String settlementStatus;

    @JsonProperty("refund_status")
    private String refundStatus;

    // 3. JAVA INVESTIGATION (Deterministic Ground Truth)
    @JsonProperty("detected_contradictions")
    private List<String> detectedContradictions;

    @JsonProperty("deterministic_findings")
    private String deterministicFindings;

    @JsonProperty("relevant_domain_signals")
    private List<String> relevantDomainSignals;

    @JsonProperty("investigation_timeline_summary")
    private List<String> investigationTimelineSummary;

    // 4. RANDOM FOREST ML RESULT
    @JsonProperty("ml_model_name")
    private String mlModelName;

    @JsonProperty("ml_predicted_class")
    private String mlPredictedClass;

    @JsonProperty("ml_confidence")
    private BigDecimal mlConfidence;

    @JsonProperty("top_ml_signals")
    private List<ContributingSignalDto> topMlSignals;

    // 5. JAVA SAFETY DECISION (Authoritative & Final)
    @JsonProperty("java_safety_decision")
    private String javaSafetyDecision;

    @JsonProperty("is_retry_prohibited")
    private boolean isRetryProhibited;

    @JsonProperty("retry_prohibition_reason")
    private String retryProhibitionReason;

    @JsonProperty("money_at_risk")
    private BigDecimal moneyAtRisk;

    @JsonProperty("is_automatic_action_allowed")
    private boolean isAutomaticActionAllowed;

    @JsonProperty("recommended_resolution")
    private String recommendedResolution;
}
