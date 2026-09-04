package com.paymentproof.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Structured response payload returned by Gemini Explanation Assistant.
 * 
 * ARCHITECTURAL PRINCIPLE:
 * JAVA INVESTIGATES. ML CLASSIFIES. JAVA DECIDES. GEMINI EXPLAINS.
 * 
 * Explanatory only; Java remains authoritative for all financial actions and states.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeminiInvestigationResponseDto {

    // Metadata separating AI Explanation from ML Assessment
    @JsonProperty("provider")
    @Builder.Default
    private String provider = "Google Gemini";

    @JsonProperty("model_used")
    private String modelUsed;

    @JsonProperty("explained_at")
    private LocalDateTime explainedAt;

    // 9 Forensic Explanation Fields
    @JsonProperty("summary")
    private String summary;

    @JsonProperty("what_happened")
    private String whatHappened;

    @JsonProperty("evidence")
    private List<String> evidence;

    @JsonProperty("contradictions")
    private List<String> contradictions;

    @JsonProperty("ml_reasoning")
    private String mlReasoning;

    @JsonProperty("uncertainty")
    private String uncertainty;

    /**
     * Explains the Java-approved action. Must NOT independently create a financial recommendation.
     */
    @JsonProperty("recommended_operator_action")
    private String recommendedOperatorAction;

    @JsonProperty("customer_impact")
    private String customerImpact;

    @JsonProperty("confidence_explanation")
    private String confidenceExplanation;
}
