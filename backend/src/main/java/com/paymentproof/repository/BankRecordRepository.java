package com.paymentproof.repository;

import com.paymentproof.entity.BankRecord;
import com.paymentproof.entity.enums.BankStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRecordRepository extends JpaRepository<BankRecord, String> {
    Optional<BankRecord> findByPaymentId(String paymentId);
    Optional<BankRecord> findByUtrNumber(String utrNumber);
    List<BankRecord> findByBankStatus(BankStatus status);
}
