package com.paymentproof.dto;

import com.paymentproof.entity.enums.BankStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankRecordDto {
    private String bankRecordId;
    private String paymentId;
    private String bankName;
    private String bankReferenceNumber;
    private String utrNumber;
    private String accountLast4;
    private BankStatus bankStatus;
    private BigDecimal debitedAmount;
    private String currency;
    private String responseCode;
    private String responseMessage;
    private Integer networkLatencyMs;
    private LocalDateTime bankTimestamp;
    private String rawPayload;
    private LocalDateTime createdAt;
}
