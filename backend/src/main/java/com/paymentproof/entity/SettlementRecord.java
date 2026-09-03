package com.paymentproof.entity;

import com.paymentproof.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_records", indexes = {
    @Index(name = "idx_set_payment", columnList = "payment_id"),
    @Index(name = "idx_set_merchant", columnList = "merchant_id"),
    @Index(name = "idx_set_batch", columnList = "batch_id"),
    @Index(name = "idx_set_status", columnList = "settlement_status"),
    @Index(name = "idx_set_settled_at", columnList = "settled_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRecord {

    @Id
    @Column(name = "settlement_id", length = 64, nullable = false)
    private String settlementId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "merchant_id", length = 64, nullable = false)
    private String merchantId;

    @Column(name = "batch_id", length = 64)
    private String batchId;

    @Column(name = "gross_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal grossAmount;

    @Column(name = "fee_deducted", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal feeDeducted = BigDecimal.ZERO;

    @Column(name = "tax_deducted", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal taxDeducted = BigDecimal.ZERO;

    @Column(name = "net_settled_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal netSettledAmount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", length = 32, nullable = false)
    private SettlementStatus settlementStatus;

    @Column(name = "settlement_utr", length = 64)
    private String settlementUtr;

    @Column(name = "settlement_bank_account", length = 32)
    private String settlementBankAccount;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
