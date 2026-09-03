package com.paymentproof.service;

import com.paymentproof.dto.ResolutionDto;
import com.paymentproof.dto.ResolutionRequestDto;
import com.paymentproof.entity.IncidentCase;
import com.paymentproof.entity.Payment;
import com.paymentproof.entity.Resolution;
import com.paymentproof.entity.enums.*;
import com.paymentproof.exception.ResourceNotFoundException;
import com.paymentproof.repository.IncidentCaseRepository;
import com.paymentproof.repository.PaymentRepository;
import com.paymentproof.repository.ResolutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResolutionService {

    private final ResolutionRepository resolutionRepository;
    private final IncidentCaseRepository incidentCaseRepository;
    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public ResolutionDto getResolutionForIncident(String incidentId) {
        Resolution resolution = resolutionRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Resolution for incident", "incidentId", incidentId));
        return mapToDto(resolution);
    }

    @Transactional
    public ResolutionDto resolveIncident(String incidentId, ResolutionRequestDto request) {
        log.info("Applying resolution to incident: {} by {}", incidentId, request.getResolvedBy());

        IncidentCase incident = incidentCaseRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentCase", "incidentId", incidentId));

        Payment payment = paymentRepository.findById(incident.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", incident.getPaymentId()));

        Resolution resolution = resolutionRepository.findByIncidentId(incidentId)
                .orElseGet(() -> Resolution.builder()
                        .resolutionId("res_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                        .incidentId(incidentId)
                        .paymentId(payment.getPaymentId())
                        .build());

        resolution.setActionTaken(request.getActionTaken());
        resolution.setResolutionType(request.getResolutionType());
        resolution.setResolvedBy(request.getResolvedBy());
        resolution.setResolutionNotes(request.getResolutionNotes());
        resolution.setFinancialImpactAmount(request.getFinancialImpactAmount() != null ? 
                request.getFinancialImpactAmount() : (request.getActionTaken() == ResolutionAction.CUSTOMER_REFUNDED ? payment.getAmount() : BigDecimal.ZERO));
        resolution.setLiabilityParty(request.getLiabilityParty() != null ? 
                request.getLiabilityParty() : LiabilityParty.PLATFORM_LOSS_NONE);
        resolution.setResolvedAt(LocalDateTime.now());

        Resolution saved = resolutionRepository.save(resolution);

        // Update incident status
        CaseStatus oldStatus = incident.getCaseStatus();
        incident.setCaseStatus(CaseStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incidentCaseRepository.save(incident);

        // Update payment status if refunded
        if (request.getActionTaken() == ResolutionAction.CUSTOMER_REFUNDED || request.getActionTaken() == ResolutionAction.DUPLICATE_REVERSED) {
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }

        // Record Chained Tamper-Evident Audit Event
        String auditAction = request.getResolutionType() == ResolutionType.OPERATOR_MANUAL_OVERRIDE
                ? "MANUAL_OVERRIDE"
                : "RESOLUTION_SELECTED";

        auditService.logEvent(
                "RESOLUTIONS",
                saved.getResolutionId(),
                auditAction,
                request.getResolutionType() == ResolutionType.OPERATOR_MANUAL_OVERRIDE ? ActorType.OPERATOR_USER : ActorType.WORKFLOW_ENGINE,
                request.getResolvedBy(),
                String.format("{\"incidentStatus\":\"%s\"}", oldStatus),
                String.format("{\"incidentStatus\":\"RESOLVED\",\"action\":\"%s\",\"financialImpact\":%s}",
                        request.getActionTaken(), resolution.getFinancialImpactAmount()),
                "127.0.0.1"
        );

        return mapToDto(saved);
    }

    public ResolutionDto mapToDto(Resolution r) {
        if (r == null) return null;
        return ResolutionDto.builder()
                .resolutionId(r.getResolutionId())
                .incidentId(r.getIncidentId())
                .paymentId(r.getPaymentId())
                .actionTaken(r.getActionTaken())
                .resolutionType(r.getResolutionType())
                .resolvedBy(r.getResolvedBy())
                .resolutionNotes(r.getResolutionNotes())
                .financialImpactAmount(r.getFinancialImpactAmount())
                .liabilityParty(r.getLiabilityParty())
                .resolvedAt(r.getResolvedAt())
                .build();
    }
}
