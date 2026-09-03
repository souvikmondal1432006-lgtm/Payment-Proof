package com.paymentproof.dto;

import com.paymentproof.entity.enums.EvidenceSource;
import com.paymentproof.entity.enums.EvidenceType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestigationEvidenceDto {
    private String evidenceId;
    private String incidentId;
    private String paymentId;
    private EvidenceSource evidenceSource;
    private EvidenceType evidenceType;
    private String filePath;
    private String rawContent;
    private String payloadChecksum;
    private LocalDateTime capturedAt;
}
