package com.paymentproof.entity;

import com.paymentproof.entity.enums.BankStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_records", indexes = {
    @Index(name = "idx_bank_payment", columnList = "payment_id"),
    @Index(name = "idx_bank_utr", columnList = "utr_number"),
    @Index(name = "idx_bank_ref", columnList = "bank_reference_number"),
    @Index(name = "idx_bank_status", columnList = "bank_status"),
    @Index(name = "idx_bank_timestamp", columnList = "bank_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankRecord {

    @Id
    @Column(name = "bank_record_id", length = 64, nullable = false)
    private String bankRecordId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "bank_name", length = 64, nullable = false)
    private String bankName;

    @Column(name = "bank_reference_number", length = 64)
    private String bankReferenceNumber;

    @Column(name = "utr_number", length = 64)
    private String utrNumber;

    @Column(name = "account_last4", length = 4)
    private String accountLast4;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_status", length = 32, nullable = false)
    private BankStatus bankStatus;

    @Column(name = "debited_amount", precision = 12, scale = 2)
    private BigDecimal debitedAmount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "response_code", length = 32)
    private String responseCode;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "network_latency_ms")
    private Integer networkLatencyMs;

    @Column(name = "bank_timestamp", nullable = false)
    private LocalDateTime bankTimestamp;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
