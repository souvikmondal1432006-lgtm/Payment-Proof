package com.paymentproof.entity;

import com.paymentproof.entity.enums.AuthStatus;
import com.paymentproof.entity.enums.CaptureStatus;
import com.paymentproof.entity.enums.GatewayStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gateway_records", indexes = {
    @Index(name = "idx_gw_payment", columnList = "payment_id"),
    @Index(name = "idx_gw_txn_id", columnList = "gateway_transaction_id"),
    @Index(name = "idx_gw_status", columnList = "gateway_status"),
    @Index(name = "idx_gw_timestamp", columnList = "gateway_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayRecord {

    @Id
    @Column(name = "gateway_record_id", length = 64, nullable = false)
    private String gatewayRecordId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "gateway_name", length = 64, nullable = false)
    private String gatewayName;

    @Column(name = "gateway_transaction_id", length = 64)
    private String gatewayTransactionId;

    @Column(name = "gateway_order_id", length = 64)
    private String gatewayOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_status", length = 32, nullable = false)
    @Builder.Default
    private AuthStatus authStatus = AuthStatus.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "capture_status", length = 32, nullable = false)
    @Builder.Default
    private CaptureStatus captureStatus = CaptureStatus.NOT_REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway_status", length = 32, nullable = false)
    private GatewayStatus gatewayStatus;

    @Column(name = "authorized_amount", precision = 12, scale = 2)
    private BigDecimal authorizedAmount;

    @Column(name = "captured_amount", precision = 12, scale = 2)
    private BigDecimal capturedAmount;

    @Column(name = "fee", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "tax", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;

    @Column(name = "processing_latency_ms")
    private Integer processingLatencyMs;

    @Column(name = "gateway_timestamp", nullable = false)
    private LocalDateTime gatewayTimestamp;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
