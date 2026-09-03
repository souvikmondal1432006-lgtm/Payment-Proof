package com.paymentproof.dto;

import com.paymentproof.entity.enums.LiabilityParty;
import com.paymentproof.entity.enums.ResolutionAction;
import com.paymentproof.entity.enums.ResolutionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolutionDto {
    private String resolutionId;
    private String incidentId;
    private String paymentId;
    private ResolutionAction actionTaken;
    private ResolutionType resolutionType;
    private String resolvedBy;
    private String resolutionNotes;
    private BigDecimal financialImpactAmount;
    private LiabilityParty liabilityParty;
    private LocalDateTime resolvedAt;
}
