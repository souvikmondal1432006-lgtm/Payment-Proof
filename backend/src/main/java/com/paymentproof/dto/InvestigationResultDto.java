package com.paymentproof.dto;

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
public class InvestigationResultDto {
    private String incidentId;
    private String paymentId;
    private String orderId;
    private String merchantId;
    private BigDecimal amount;
    private String paymentMethod;
    
    // Investigation Core Attributes
    private IncidentType incidentClassification;
    private Severity severity;
    private CaseStatus investigationStatus;
    private BigDecimal confidence;
    private BigDecimal anomalyScore;
    private String predictedRootCause;
    private SuggestedAction recommendedAction;
    private String modelVersion;
    private String modelExplanation;
    
    // Contradictions & Evidence
    private List<String> contradictionsDetected;
    private int evidenceCount;
    private List<InvestigationEvidenceDto> evidenceList;
    private List<ContributingSignalDto> topContributingSignals;
    private Map<String, BigDecimal> classProbabilities;
    
    // Safety & Financial Invariants
    private boolean isRetryProhibited;
    private String retryProhibitionReason;
    private BigDecimal moneyAtRisk;
    
    // Multi-Party System State Snapshot
    private String bankStatus;
    private String bankUtr;
    private String gatewayStatus;
    private String gatewayAuthStatus;
    private String gatewayCaptureStatus;
    private String merchantOrderStatus;
    private String merchantFulfillmentStatus;
    private String webhookDeliveryStatus;
    private Integer webhookHttpStatusCode;
    private String settlementStatus;
    private String refundStatus;

    // AI Investigation Narrative & Structured Report
    private AiInvestigationReportDto aiReport;
    private GeminiInvestigationResponseDto geminiExplanation;

    // Timing & Metadata
    private LocalDateTime investigatedAt;
    private String summary;

    @com.fasterxml.jackson.annotation.JsonProperty("caseStatus")
    public CaseStatus getCaseStatus() {
        return investigationStatus;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isRetryProhibited")
    public boolean getIsRetryProhibited() {
        return isRetryProhibited;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("retryReason")
    public String getRetryReason() {
        return retryProhibitionReason;
    }
}
