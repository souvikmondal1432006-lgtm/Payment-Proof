package com.paymentproof.service;

import com.paymentproof.dto.DashboardSummaryDto;
import com.paymentproof.entity.enums.CaseStatus;
import com.paymentproof.entity.enums.PaymentStatus;
import com.paymentproof.entity.enums.Severity;
import com.paymentproof.repository.IncidentCaseRepository;
import com.paymentproof.repository.PaymentRepository;
import com.paymentproof.repository.ResolutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PaymentRepository paymentRepository;
    private final IncidentCaseRepository incidentCaseRepository;
    private final ResolutionRepository resolutionRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        long totalPayments = paymentRepository.count();
        long successfulPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long failedPayments = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long disputedPayments = paymentRepository.countByStatus(PaymentStatus.DISPUTED);
        long flaggedPayments = paymentRepository.countByStatus(PaymentStatus.FLAGGED);

        long totalIncidents = incidentCaseRepository.count();
        long openIncidents = incidentCaseRepository.countByCaseStatus(CaseStatus.OPEN) +
                              incidentCaseRepository.countByCaseStatus(CaseStatus.IN_REVIEW) +
                              incidentCaseRepository.countByCaseStatus(CaseStatus.AI_ANALYZED);
        long criticalIncidents = incidentCaseRepository.countBySeverity(Severity.CRITICAL);
        long resolvedIncidents = incidentCaseRepository.countByCaseStatus(CaseStatus.RESOLVED);
        long escalatedIncidents = incidentCaseRepository.countByCaseStatus(CaseStatus.ESCALATED_TO_BANK);

        BigDecimal resolvedFinancialImpact = resolutionRepository.sumTotalFinancialImpact();
        if (resolvedFinancialImpact == null) {
            resolvedFinancialImpact = BigDecimal.ZERO;
        }

        // Compute volume and approximate money at risk from open incidents
        BigDecimal totalVolume = paymentRepository.findAll().stream()
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal moneyAtRisk = incidentCaseRepository.findByCaseStatus(CaseStatus.OPEN).stream()
                .map(i -> paymentRepository.findById(i.getPaymentId())
                        .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                        .orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Grouped statistics
        Map<String, Long> incidentsByType = new HashMap<>();
        List<Object[]> typeCounts = incidentCaseRepository.countGroupedByIncidentType();
        for (Object[] row : typeCounts) {
            if (row[0] != null) {
                incidentsByType.put(row[0].toString(), (Long) row[1]);
            }
        }

        Map<String, Long> incidentsBySeverity = new HashMap<>();
        List<Object[]> sevCounts = incidentCaseRepository.countGroupedBySeverity();
        for (Object[] row : sevCounts) {
            if (row[0] != null) {
                incidentsBySeverity.put(row[0].toString(), (Long) row[1]);
            }
        }

        Map<String, Long> incidentsByStatus = new HashMap<>();
        List<Object[]> statusCounts = incidentCaseRepository.countGroupedByCaseStatus();
        for (Object[] row : statusCounts) {
            if (row[0] != null) {
                incidentsByStatus.put(row[0].toString(), (Long) row[1]);
            }
        }

        Map<String, Long> resolutionsByAction = new HashMap<>();
        List<Object[]> actionCounts = resolutionRepository.countGroupedByAction();
        for (Object[] row : actionCounts) {
            if (row[0] != null) {
                resolutionsByAction.put(row[0].toString(), (Long) row[1]);
            }
        }

        return DashboardSummaryDto.builder()
                .totalPayments(totalPayments)
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .disputedPayments(disputedPayments)
                .flaggedPayments(flaggedPayments)
                .totalIncidents(totalIncidents)
                .openIncidents(openIncidents)
                .criticalIncidents(criticalIncidents)
                .resolvedIncidents(resolvedIncidents)
                .escalatedIncidents(escalatedIncidents)
                .totalVolumeProcessed(totalVolume)
                .totalMoneyAtRisk(moneyAtRisk)
                .totalFinancialImpactResolved(resolvedFinancialImpact)
                .incidentsByType(incidentsByType)
                .incidentsBySeverity(incidentsBySeverity)
                .incidentsByStatus(incidentsByStatus)
                .resolutionsByAction(resolutionsByAction)
                .build();
    }
}
