package com.paymentproof.repository;

import com.paymentproof.entity.GatewayRecord;
import com.paymentproof.entity.enums.GatewayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GatewayRecordRepository extends JpaRepository<GatewayRecord, String> {
    Optional<GatewayRecord> findByPaymentId(String paymentId);
    Optional<GatewayRecord> findByGatewayTransactionId(String gatewayTransactionId);
    List<GatewayRecord> findByGatewayStatus(GatewayStatus status);
}
