package com.paymentproof.dto;

import com.paymentproof.entity.enums.ActorType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventDto {
    private String auditId;
    private Long sequenceNumber;
    private String entityName;
    private String entityId;
    private String action;
    private ActorType actorType;
    private String actorId;
    private String previousState;
    private String newState;
    private String ipAddress;
    private String previousEventHash;
    private String currentEventHash;
    private LocalDateTime createdAt;
}
