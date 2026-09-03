package com.paymentproof.service;

import com.paymentproof.dto.AuditEventDto;
import com.paymentproof.dto.AuditVerificationResultDto;
import com.paymentproof.entity.AuditEvent;
import com.paymentproof.entity.enums.ActorType;
import com.paymentproof.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final AuditChainService auditChainService;

    @Transactional
    public AuditEvent logEvent(String entityName, String entityId, String action,
                              ActorType actorType, String actorId, String previousState, String newState, String ipAddress) {

        return auditChainService.recordChainedEvent(
                entityName,
                entityId,
                action,
                actorType,
                actorId,
                previousState,
                newState,
                ipAddress
        );
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getAllAuditEvents() {
        return auditEventRepository.findAllByOrderBySequenceNumberAsc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getAuditTrailForEntity(String entityId) {
        return auditEventRepository.findByEntityIdOrderBySequenceNumberAsc(entityId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AuditVerificationResultDto verifyAuditLedgerIntegrity() {
        return auditChainService.verifyAuditChainIntegrity();
    }

    public AuditEventDto mapToDto(AuditEvent event) {
        if (event == null) return null;
        return AuditEventDto.builder()
                .auditId(event.getAuditId())
                .sequenceNumber(event.getSequenceNumber())
                .entityName(event.getEntityName())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .actorType(event.getActorType())
                .actorId(event.getActorId())
                .previousState(event.getPreviousState())
                .newState(event.getNewState())
                .ipAddress(event.getIpAddress())
                .previousEventHash(event.getPreviousEventHash())
                .currentEventHash(event.getCurrentEventHash())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
