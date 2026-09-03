package com.paymentproof.repository;

import com.paymentproof.entity.MlAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MlAssessmentRepository extends JpaRepository<MlAssessment, String> {
    Optional<MlAssessment> findByIncidentId(String incidentId);
    List<MlAssessment> findByPaymentId(String paymentId);
}
