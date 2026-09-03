package com.paymentproof.dto;

import com.paymentproof.entity.enums.SettlementStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRecordDto {
    private String settlementId;
    private String paymentId;
    private String merchantId;
    private String batchId;
    private BigDecimal grossAmount;
    private BigDecimal feeDeducted;
    private BigDecimal taxDeducted;
    private BigDecimal netSettledAmount;
    private String currency;
    private SettlementStatus settlementStatus;
    private String settlementUtr;
    private String settlementBankAccount;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
