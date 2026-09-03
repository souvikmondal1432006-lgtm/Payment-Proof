package com.paymentproof.dto;

import com.paymentproof.entity.enums.BankReversalStatus;
import com.paymentproof.entity.enums.RefundSpeed;
import com.paymentproof.entity.enums.RefundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRecordDto {
    private String refundId;
    private String paymentId;
    private String merchantId;
    private String gatewayRefundId;
    private String refundArn;
    private BigDecimal amount;
    private String currency;
    private String refundReason;
    private RefundSpeed refundSpeed;
    private RefundStatus refundStatus;
    private BankReversalStatus bankReversalStatus;
    private LocalDateTime initiatedAt;
    private LocalDateTime processedAt;
    private String rawResponse;
}
