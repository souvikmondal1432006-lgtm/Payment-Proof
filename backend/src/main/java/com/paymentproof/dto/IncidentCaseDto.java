package com.paymentproof.dto;

import com.paymentproof.entity.enums.CaseStatus;
import com.paymentproof.entity.enums.IncidentType;
import com.paymentproof.entity.enums.Severity;
import com.paymentproof.entity.enums.SuggestedAction;
import com.paymentproof.entity.enums.TriggerSource;
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
public class IncidentCaseDto {
    private String incidentId;
    private String paymentId;
    private IncidentType incidentType;
    private Severity severity;
    private CaseStatus caseStatus;
    private TriggerSource triggerSource;
    private String assignedInvestigator;
    private String title;
    private String description;
    private LocalDateTime openedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime updatedAt;

    // Enriched Payment Attributes
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String orderId;
    private String merchantId;
    private String customerId;

    // Financial Safety & Invariants
    private BigDecimal moneyAtRisk;
    private boolean isRetryProhibited;
    private String retryProhibitionReason;

    // ML & Classification
    private String predictedRootCause;
    private BigDecimal confidence;
    private BigDecimal anomalyScore;
    private SuggestedAction recommendedAction;

    // Multi-Party Evidence Telemetry Nodes
    private Map<String, Object> bank;
    private Map<String, Object> gateway;
    private Map<String, Object> merchant;
    private Map<String, Object> webhook;
    private Map<String, Object> settlement;
    private Map<String, Object> refund;

    // Contradictions & Reports
    private List<String> contradictions;
    private AiInvestigationReportDto aiReport;
    private GeminiInvestigationResponseDto geminiExplanation;
}
