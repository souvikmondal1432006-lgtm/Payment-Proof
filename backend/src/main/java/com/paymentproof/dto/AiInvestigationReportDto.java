package com.paymentproof.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paymentproof.entity.enums.CaseStatus;
import com.paymentproof.entity.enums.IncidentType;
import com.paymentproof.entity.enums.Severity;
import com.paymentproof.entity.enums.SuggestedAction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInvestigationReportDto {

    private String incidentId;
    private String paymentId;
    private String orderId;
    private String merchantId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private IncidentType incidentClassification;
    private Severity severity;
    private CaseStatus investigationStatus;

    // --- MANDATORY AI REPORT SECTIONS ---
    @JsonProperty("what_happened")
    private String whatHappened;

    @JsonProperty("why_we_think_this")
    private String whyWeThinkThis;

    @JsonProperty("evidence")
    private List<InvestigationEvidenceDto> evidence;

    @JsonProperty("what_is_uncertain")
    private String whatIsUncertain;

    @JsonProperty("recommended_action")
    private SuggestedAction recommendedAction;

    @JsonProperty("money_at_risk")
    private BigDecimal moneyAtRisk;

    @JsonProperty("confidence")
    private BigDecimal confidence;

    // --- SAFETY & AUDIT ATTRIBUTES ---
    @JsonProperty("is_retry_prohibited")
    private boolean isRetryProhibited;

    @JsonProperty("retry_prohibition_reason")
    private String retryProhibitionReason;

    @JsonProperty("decision_factors")
    private List<String> decisionFactors;

    @JsonProperty("contradictions_detected")
    private List<String> contradictionsDetected;

    @JsonProperty("top_contributing_signals")
    private List<ContributingSignalDto> topContributingSignals;

    @JsonProperty("class_probabilities")
    private Map<String, BigDecimal> classProbabilities;

    @JsonProperty("timeline_events_count")
    private int timelineEventsCount;

    @JsonProperty("generated_at")
    private LocalDateTime generatedAt;
}
