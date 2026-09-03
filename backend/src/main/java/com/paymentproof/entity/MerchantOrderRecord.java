package com.paymentproof.entity;

import com.paymentproof.entity.enums.FulfillmentStatus;
import com.paymentproof.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_order_records", indexes = {
    @Index(name = "idx_mor_payment", columnList = "payment_id"),
    @Index(name = "idx_mor_order", columnList = "merchant_order_id"),
    @Index(name = "idx_mor_merchant", columnList = "merchant_id"),
    @Index(name = "idx_mor_status", columnList = "order_status"),
    @Index(name = "idx_mor_fulfillment", columnList = "fulfillment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantOrderRecord {

    @Id
    @Column(name = "merchant_order_record_id", length = 64, nullable = false)
    private String merchantOrderRecordId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "merchant_id", length = 64, nullable = false)
    private String merchantId;

    @Column(name = "merchant_order_id", length = 64, nullable = false)
    private String merchantOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 32, nullable = false)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", length = 32, nullable = false)
    @Builder.Default
    private FulfillmentStatus fulfillmentStatus = FulfillmentStatus.UNFULFILLED;

    @Column(name = "expected_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal expectedAmount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    @Column(name = "merchant_updated_at", nullable = false)
    private LocalDateTime merchantUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
