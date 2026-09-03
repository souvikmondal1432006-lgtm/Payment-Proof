package com.paymentproof.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditVerificationResultDto {
    private boolean isValid;
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
