package com.paymentproof.entity;

import com.paymentproof.entity.enums.WebhookDeliveryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_records", indexes = {
    @Index(name = "idx_wh_payment", columnList = "payment_id"),
    @Index(name = "idx_wh_merchant", columnList = "merchant_id"),
    @Index(name = "idx_wh_status", columnList = "delivery_status"),
    @Index(name = "idx_wh_event", columnList = "event_name"),
    @Index(name = "idx_wh_first_attempt", columnList = "first_attempt_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookRecord {

    @Id
    @Column(name = "webhook_id", length = 64, nullable = false)
    private String webhookId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "merchant_id", length = 64, nullable = false)
    private String merchantId;

    @Column(name = "event_name", length = 64, nullable = false)
    private String eventName;

    @Column(name = "target_url", length = 512, nullable = false)
    private String targetUrl;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 1;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 32, nullable = false)
    private WebhookDeliveryStatus deliveryStatus;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "request_payload_hash", length = 64)
    private String requestPayloadHash;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "first_attempt_at", nullable = false)
    private LocalDateTime firstAttemptAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
