package com.paymentproof.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MlAssessmentDto {

    private String assessmentId;
    private String incidentId;
    private String paymentId;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("classification")
    private String classification;

    @JsonProperty("predicted_root_cause")
    private String predictedRootCause;

    @JsonProperty("anomaly_score")
    private BigDecimal anomalyScore;

    @JsonProperty("confidence")
    private BigDecimal confidence;

    @JsonProperty("confidence_score")
    private BigDecimal confidenceScore;

    @JsonProperty("suggested_action")
    private SuggestedAction suggestedAction;

    @JsonProperty("recommended_action")
    private String recommendedAction;

    @JsonProperty("top_contributing_signals")
    private List<ContributingSignalDto> topContributingSignals;

    @JsonProperty("class_probabilities")
    private Map<String, BigDecimal> classProbabilities;

    @JsonProperty("is_retry_prohibited_recommendation")
    private Boolean isRetryProhibitedRecommendation;

    private String featureSnapshot;

    @JsonProperty("model_explanation")
    private String modelExplanation;

    private LocalDateTime assessedAt;

    public BigDecimal getEffectiveConfidence() {
        if (confidenceScore != null) {
            return confidenceScore;
        }
        return confidence != null ? confidence : BigDecimal.ZERO;
    }

    public String getEffectivePredictedClass() {
        if (predictedRootCause != null && !predictedRootCause.isBlank()) {
            return predictedRootCause;
        }
        return classification != null ? classification : "UNKNOWN";
    }
}
