package com.paymentproof.entity;

import com.paymentproof.entity.enums.EvidenceSource;
import com.paymentproof.entity.enums.EvidenceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "investigation_evidence", indexes = {
    @Index(name = "idx_evi_incident", columnList = "incident_id"),
    @Index(name = "idx_evi_payment", columnList = "payment_id"),
    @Index(name = "idx_evi_source", columnList = "evidence_source")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationEvidence {

    @Id
    @Column(name = "evidence_id", length = 64, nullable = false)
    private String evidenceId;

    @Column(name = "incident_id", length = 64, nullable = false)
    private String incidentId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_source", length = 32, nullable = false)
    private EvidenceSource evidenceSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", length = 32, nullable = false)
    private EvidenceType evidenceType;

    @Column(name = "file_path", length = 255)
    private String filePath;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "payload_checksum", length = 64)
    private String payloadChecksum;

    @CreationTimestamp
    @Column(name = "captured_at", nullable = false, updatable = false)
    private LocalDateTime capturedAt;
}
