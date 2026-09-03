package com.paymentproof.repository;

import com.paymentproof.entity.IncidentCase;
import com.paymentproof.entity.enums.CaseStatus;
import com.paymentproof.entity.enums.IncidentType;
import com.paymentproof.entity.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentCaseRepository extends JpaRepository<IncidentCase, String> {

    List<IncidentCase> findByPaymentId(String paymentId);

    Optional<IncidentCase> findFirstByPaymentIdOrderByOpenedAtDesc(String paymentId);

    List<IncidentCase> findByCaseStatus(CaseStatus caseStatus);

    List<IncidentCase> findBySeverity(Severity severity);

    List<IncidentCase> findByIncidentType(IncidentType incidentType);

    @Query("SELECT i FROM IncidentCase i WHERE " +
           "(:caseStatus IS NULL OR i.caseStatus = :caseStatus) AND " +
           "(:severity IS NULL OR i.severity = :severity) AND " +
           "(:incidentType IS NULL OR i.incidentType = :incidentType) AND " +
           "(:search IS NULL OR LOWER(i.incidentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(i.paymentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(i.assignedInvestigator) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<IncidentCase> findWithFilters(
            @Param("caseStatus") CaseStatus caseStatus,
            @Param("severity") Severity severity,
            @Param("incidentType") IncidentType incidentType,
            @Param("search") String search,
            Pageable pageable
    );

    long countByCaseStatus(CaseStatus status);

    long countBySeverity(Severity severity);

    long countByIncidentType(IncidentType incidentType);

    @Query("SELECT i.incidentType, COUNT(i) FROM IncidentCase i GROUP BY i.incidentType")
    List<Object[]> countGroupedByIncidentType();

    @Query("SELECT i.severity, COUNT(i) FROM IncidentCase i GROUP BY i.severity")
    List<Object[]> countGroupedBySeverity();

    @Query("SELECT i.caseStatus, COUNT(i) FROM IncidentCase i GROUP BY i.caseStatus")
    List<Object[]> countGroupedByCaseStatus();
}
