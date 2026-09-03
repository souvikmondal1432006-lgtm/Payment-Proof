package com.paymentproof.repository;

import com.paymentproof.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, String> {
    List<PaymentEvent> findByPaymentIdOrderByEventTimestampAsc(String paymentId);
}
