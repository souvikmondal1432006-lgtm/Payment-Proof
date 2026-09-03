package com.paymentproof.repository;

import com.paymentproof.entity.WebhookRecord;
import com.paymentproof.entity.enums.WebhookDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookRecordRepository extends JpaRepository<WebhookRecord, String> {
    Optional<WebhookRecord> findByPaymentId(String paymentId);
    List<WebhookRecord> findByDeliveryStatus(WebhookDeliveryStatus status);
}
