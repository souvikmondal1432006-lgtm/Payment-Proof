package com.paymentproof.entity;

import com.paymentproof.entity.enums.CaseStatus;
import com.paymentproof.entity.enums.IncidentType;
import com.paymentproof.entity.enums.Severity;
import com.paymentproof.entity.enums.TriggerSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_cases", indexes = {
    @Index(name = "idx_inc_payment", columnList = "payment_id"),
    @Index(name = "idx_inc_type", columnList = "incident_type"),
    @Index(name = "idx_inc_status", columnList = "case_status"),
    @Index(name = "idx_inc_severity", columnList = "severity"),
    @Index(name = "idx_inc_opened_at", columnList = "opened_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentCase {

    @Id
    @Column(name = "incident_id", length = 64, nullable = false)
    private String incidentId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", length = 64, nullable = false)
    private IncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 32, nullable = false)
    @Builder.Default
    private Severity severity = Severity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_status", length = 32, nullable = false)
    @Builder.Default
    private CaseStatus caseStatus = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_source", length = 64, nullable = false)
    private TriggerSource triggerSource;

    @Column(name = "assigned_investigator", length = 64)
    private String assignedInvestigator;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
