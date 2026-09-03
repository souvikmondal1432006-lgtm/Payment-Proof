package com.paymentproof.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    private long totalPayments;
    private long successfulPayments;
    private long failedPayments;
    private long disputedPayments;
    private long flaggedPayments;

    private long totalIncidents;
    private long openIncidents;
    private long criticalIncidents;
    private long resolvedIncidents;
    private long escalatedIncidents;

    private BigDecimal totalVolumeProcessed;
    private BigDecimal totalMoneyAtRisk;
    private BigDecimal totalFinancialImpactResolved;

    private Map<String, Long> incidentsByType;
    private Map<String, Long> incidentsBySeverity;
    private Map<String, Long> incidentsByStatus;
    private Map<String, Long> resolutionsByAction;
}
