package com.paymentproof.entity;

import com.paymentproof.entity.enums.LiabilityParty;
import com.paymentproof.entity.enums.ResolutionAction;
import com.paymentproof.entity.enums.ResolutionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resolutions", indexes = {
    @Index(name = "idx_res_incident", columnList = "incident_id"),
    @Index(name = "idx_res_payment", columnList = "payment_id"),
    @Index(name = "idx_res_action", columnList = "action_taken"),
    @Index(name = "idx_res_type", columnList = "resolution_type"),
    @Index(name = "idx_res_resolved_at", columnList = "resolved_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resolution {

    @Id
    @Column(name = "resolution_id", length = 64, nullable = false)
    private String resolutionId;

    @Column(name = "incident_id", length = 64, nullable = false, unique = true)
    private String incidentId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", length = 64, nullable = false)
    private ResolutionAction actionTaken;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", length = 64, nullable = false)
    private ResolutionType resolutionType;

    @Column(name = "resolved_by", length = 64, nullable = false)
    private String resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "TEXT", nullable = false)
    private String resolutionNotes;

    @Column(name = "financial_impact_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal financialImpactAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "liability_party", length = 64, nullable = false)
    @Builder.Default
    private LiabilityParty liabilityParty = LiabilityParty.PLATFORM_LOSS_NONE;

    @CreationTimestamp
    @Column(name = "resolved_at", nullable = false, updatable = false)
    private LocalDateTime resolvedAt;
}
