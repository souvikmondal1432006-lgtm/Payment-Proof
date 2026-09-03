package com.paymentproof.repository;

import com.paymentproof.entity.InvestigationEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestigationEvidenceRepository extends JpaRepository<InvestigationEvidence, String> {
    List<InvestigationEvidence> findByIncidentId(String incidentId);
    List<InvestigationEvidence> findByPaymentId(String paymentId);
    long countByIncidentId(String incidentId);
}
