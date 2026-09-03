package com.paymentproof.repository;

import com.paymentproof.entity.SettlementRecord;
import com.paymentproof.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, String> {
    Optional<SettlementRecord> findByPaymentId(String paymentId);
    List<SettlementRecord> findBySettlementStatus(SettlementStatus status);
}
