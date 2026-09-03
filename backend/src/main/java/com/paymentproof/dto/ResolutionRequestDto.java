package com.paymentproof.dto;

import com.paymentproof.entity.enums.LiabilityParty;
import com.paymentproof.entity.enums.ResolutionAction;
import com.paymentproof.entity.enums.ResolutionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolutionRequestDto {

    @NotNull(message = "actionTaken is required")
    private ResolutionAction actionTaken;

    @NotNull(message = "resolutionType is required")
    private ResolutionType resolutionType;

    @NotBlank(message = "resolvedBy is required")
    private String resolvedBy;

    @NotBlank(message = "resolutionNotes is required")
    private String resolutionNotes;

    private BigDecimal financialImpactAmount;

    private LiabilityParty liabilityParty;
}
