package com.paymentproof.entity;

import com.paymentproof.entity.enums.SuggestedAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ml_assessments", indexes = {
    @Index(name = "idx_mla_incident", columnList = "incident_id"),
    @Index(name = "idx_mla_payment", columnList = "payment_id"),
    @Index(name = "idx_mla_anomaly", columnList = "anomaly_score"),
    @Index(name = "idx_mla_confidence", columnList = "confidence_score")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlAssessment {

    @Id
    @Column(name = "assessment_id", length = 64, nullable = false)
    private String assessmentId;

    @Column(name = "incident_id", length = 64, nullable = false)
    private String incidentId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "model_version", length = 64, nullable = false)
    private String modelVersion;

    @Column(name = "predicted_root_cause", length = 128, nullable = false)
    private String predictedRootCause;

    @Column(name = "anomaly_score", precision = 5, scale = 4, nullable = false)
    private BigDecimal anomalyScore;

    @Column(name = "confidence_score", precision = 5, scale = 4, nullable = false)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_action", length = 64, nullable = false)
    private SuggestedAction suggestedAction;

    @Column(name = "feature_snapshot", columnDefinition = "TEXT")
    private String featureSnapshot;

    @Column(name = "model_explanation", columnDefinition = "TEXT")
    private String modelExplanation;

    @CreationTimestamp
    @Column(name = "assessed_at", nullable = false, updatable = false)
    private LocalDateTime assessedAt;
}
