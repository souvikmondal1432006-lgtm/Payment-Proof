package com.paymentproof.entity;

import com.paymentproof.entity.enums.ActorType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_aud_entity", columnList = "entity_name, entity_id"),
    @Index(name = "idx_aud_seq", columnList = "sequence_number"),
    @Index(name = "idx_aud_action", columnList = "action"),
    @Index(name = "idx_aud_actor", columnList = "actor_type, actor_id"),
    @Index(name = "idx_aud_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @Column(name = "audit_id", length = 64, nullable = false)
    private String auditId;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "entity_name", length = 32, nullable = false)
    private String entityName;

    @Column(name = "entity_id", length = 64, nullable = false)
    private String entityId;

    @Column(name = "action", length = 64, nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 32, nullable = false)
    private ActorType actorType;

    @Column(name = "actor_id", length = 64, nullable = false)
    private String actorId;

    @Column(name = "previous_state", columnDefinition = "TEXT")
    private String previousState;

    @Column(name = "new_state", columnDefinition = "TEXT")
    private String newState;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "previous_event_hash", length = 64, nullable = false)
    private String previousEventHash;

    @Column(name = "current_event_hash", length = 64, nullable = false)
    private String currentEventHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
