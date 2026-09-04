package com.paymentproof.service;

import com.paymentproof.dto.ResolutionDto;
import com.paymentproof.dto.ResolutionRequestDto;
import com.paymentproof.entity.BankRecord;
import com.paymentproof.entity.IncidentCase;
import com.paymentproof.entity.Payment;
import com.paymentproof.entity.Resolution;
import com.paymentproof.entity.enums.*;
import com.paymentproof.exception.InvalidOperationException;
import com.paymentproof.exception.ResourceNotFoundException;
import com.paymentproof.repository.BankRecordRepository;
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
    private final BankRecordRepository bankRecordRepository;
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

        if (request.getActionTaken() == null) {
            throw new InvalidOperationException("Resolution actionTaken is required.");
        }

        if (request.getResolutionType() == null) {
            request.setResolutionType(ResolutionType.OPERATOR_MANUAL_OVERRIDE);
        }

        if (request.getResolutionNotes() == null || request.getResolutionNotes().isBlank()) {
            request.setResolutionNotes("Authoritative resolution executed by " + 
                    (request.getResolvedBy() != null ? request.getResolvedBy() : "operator") + ".");
        }

        // Safety Invariant 1: External advisory models (ML / Gemini) cannot authorize or execute resolutions
        if (request.getResolvedBy() != null && (
                request.getResolvedBy().toUpperCase().contains("GEMINI") ||
                request.getResolvedBy().toUpperCase().contains("ML_SERVICE") ||
                request.getResolvedBy().toUpperCase().contains("PYTHON_ML") ||
                request.getResolvedBy().toUpperCase().contains("RANDOM_FOREST"))) {
            throw new InvalidOperationException("External advisory models (ML / Gemini) cannot independently authorize or execute resolutions. Only Java workflows or human operators are permitted.");
        }

        IncidentCase incident = incidentCaseRepository.findById(incidentId)
                .or(() -> incidentCaseRepository.findFirstByPaymentIdOrderByOpenedAtDesc(incidentId))
                .orElseThrow(() -> new ResourceNotFoundException("IncidentCase", "incidentId or paymentId", incidentId));

        // Safety Invariant 2: If incident is under NEEDS_REVIEW, automated resolution is prohibited; operator manual override is required
        if (incident.getCaseStatus() == CaseStatus.NEEDS_REVIEW && request.getResolutionType() != ResolutionType.OPERATOR_MANUAL_OVERRIDE) {
            throw new InvalidOperationException("Incident is under NEEDS_REVIEW status due to low ML confidence or contradictory evidence. Automated resolution is prohibited; manual operator review is required.");
        }

        // Safety Invariant 3: Money Lock / Active Debit Invariant
        // If bank debited the customer, resolving as NO_DISCREPANCY_FOUND without refund or justification is strictly prohibited
        BankRecord bank = bankRecordRepository.findByPaymentId(incident.getPaymentId()).orElse(null);
        boolean isBankDebited = (bank != null && (bank.getBankStatus() == BankStatus.SUCCESS || bank.getBankStatus() == BankStatus.DEBITED));
        if (isBankDebited && request.getActionTaken() == ResolutionAction.NO_DISCREPANCY_FOUND) {
            throw new InvalidOperationException("STRICT SAFETY INVARIANT VIOLATION: Customer account was confirmed debited (UTR: " + 
                    (bank != null ? bank.getUtrNumber() : "UNKNOWN") + "). Cannot close incident with NO_DISCREPANCY_FOUND without customer remediation.");
        }

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
        String auditAction = "RESOLUTION_CREATED";

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
