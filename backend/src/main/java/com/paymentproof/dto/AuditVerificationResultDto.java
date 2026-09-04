package com.paymentproof.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditVerificationResultDto {
    @JsonProperty("isValid")
    private boolean isValid;

    public boolean isValid() {
        return isValid;
    }

    @JsonProperty("isValid")
    public boolean getIsValid() {
        return isValid;
    }
    private int totalEventsVerified;
    private String genesisHash;
    private String latestHeadHash;
    private String tamperedAuditId;
    private Long tamperedAtSequence;
    private String expectedHash;
    private String actualHash;
    private String verificationSummary;
    private LocalDateTime verifiedAt;
}
