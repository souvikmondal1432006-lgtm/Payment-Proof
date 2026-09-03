package com.paymentproof.dto;

import com.paymentproof.entity.enums.CaseStatus;
import com.paymentproof.entity.enums.IncidentType;
import com.paymentproof.entity.enums.Severity;
import com.paymentproof.entity.enums.TriggerSource;
import lombok.*;

import java.time.LocalDateTime;

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
}
