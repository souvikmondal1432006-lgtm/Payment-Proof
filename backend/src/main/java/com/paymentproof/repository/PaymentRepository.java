package com.paymentproof.repository;

import com.paymentproof.entity.Payment;
import com.paymentproof.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findByMerchantId(String merchantId);

    List<Payment> findByCustomerId(String customerId);

    List<Payment> findByStatus(PaymentStatus status);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Page<Payment> findByMerchantId(String merchantId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE " +
           "(:merchantId IS NULL OR p.merchantId = :merchantId) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod) AND " +
           "(:search IS NULL OR LOWER(p.paymentId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.orderId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(p.customerId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Payment> findWithFilters(
            @Param("merchantId") String merchantId,
            @Param("status") PaymentStatus status,
            @Param("paymentMethod") String paymentMethod,
            @Param("search") String search,
            Pageable pageable
    );

    long countByStatus(PaymentStatus status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.initiatedAt >= :since")
    long countRecentPayments(@Param("since") LocalDateTime since);
}
