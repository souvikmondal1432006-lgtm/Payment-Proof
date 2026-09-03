package com.paymentproof.repository;

import com.paymentproof.entity.MerchantOrderRecord;
import com.paymentproof.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantOrderRecordRepository extends JpaRepository<MerchantOrderRecord, String> {
    Optional<MerchantOrderRecord> findByPaymentId(String paymentId);
    List<MerchantOrderRecord> findByMerchantOrderId(String merchantOrderId);
    List<MerchantOrderRecord> findByOrderStatus(OrderStatus status);
}
