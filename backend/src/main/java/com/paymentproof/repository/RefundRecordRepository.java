package com.paymentproof.repository;

import com.paymentproof.entity.RefundRecord;
import com.paymentproof.entity.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRecordRepository extends JpaRepository<RefundRecord, String> {
    Optional<RefundRecord> findByPaymentId(String paymentId);
    List<RefundRecord> findByRefundStatus(RefundStatus status);
}
