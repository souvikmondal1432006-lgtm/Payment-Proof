package com.paymentproof.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_events", indexes = {
    @Index(name = "idx_pevt_payment", columnList = "payment_id"),
    @Index(name = "idx_pevt_type", columnList = "event_type"),
    @Index(name = "idx_pevt_timestamp", columnList = "event_timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {

    @Id
    @Column(name = "event_id", length = 64, nullable = false)
    private String eventId;

    @Column(name = "payment_id", length = 64, nullable = false)
    private String paymentId;

    @Column(name = "event_type", length = 64, nullable = false)
    private String eventType;

    @Column(name = "from_status", length = 32)
    private String fromStatus;

    @Column(name = "to_status", length = 32, nullable = false)
    private String toStatus;

    @Column(name = "event_source", length = 32, nullable = false)
    private String eventSource;

    @Column(name = "event_payload", columnDefinition = "TEXT")
    private String eventPayload;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;
}
