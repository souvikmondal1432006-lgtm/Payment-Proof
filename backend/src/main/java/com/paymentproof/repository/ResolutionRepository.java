package com.paymentproof.repository;

import com.paymentproof.entity.Resolution;
import com.paymentproof.entity.enums.LiabilityParty;
import com.paymentproof.entity.enums.ResolutionAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResolutionRepository extends JpaRepository<Resolution, String> {
    Optional<Resolution> findByIncidentId(String incidentId);
    List<Resolution> findByPaymentId(String paymentId);
    List<Resolution> findByActionTaken(ResolutionAction actionTaken);
    List<Resolution> findByLiabilityParty(LiabilityParty liabilityParty);

    @Query("SELECT SUM(r.financialImpactAmount) FROM Resolution r")
    BigDecimal sumTotalFinancialImpact();

    @Query("SELECT r.actionTaken, COUNT(r) FROM Resolution r GROUP BY r.actionTaken")
    List<Object[]> countGroupedByAction();

    @Query("SELECT r.liabilityParty, COUNT(r) FROM Resolution r GROUP BY r.liabilityParty")
    List<Object[]> countGroupedByLiabilityParty();
}
