package com.paymentproof.entity;

import com.paymentproof.entity.enums.BankReversalStatus;
import com.paymentproof.entity.enums.RefundSpeed;
import com.paymentproof.entity.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_records", indexes = {
    @Index(name = "idx_ref_payment", columnList = "payment_id"),
    @Index(name = "idx_ref_merchant", columnList = "merchant_id"),
    @Index(name = "idx_ref_status", columnList = "refund_status"),
    @Index(name = "idx_ref_arn", columnList = "refund_arn"),
    @Index(name = "idx_ref_initiated", columnList = "initiated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRecord {

    @Id
    @Column(name = "refund_id", length = 64, nullable = false)
    private String refundId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "merchant_id", length = 64, nullable = false)
    private String merchantId;

    @Column(name = "gateway_refund_id", length = 64)
    private String gatewayRefundId;

    @Column(name = "refund_arn", length = 64)
    private String refundArn;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "refund_reason", length = 255, nullable = false)
    private String refundReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_speed", length = 32, nullable = false)
    @Builder.Default
    private RefundSpeed refundSpeed = RefundSpeed.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 32, nullable = false)
    private RefundStatus refundStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_reversal_status", length = 32, nullable = false)
    @Builder.Default
    private BankReversalStatus bankReversalStatus = BankReversalStatus.NOT_INITIATED;

    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;
}
