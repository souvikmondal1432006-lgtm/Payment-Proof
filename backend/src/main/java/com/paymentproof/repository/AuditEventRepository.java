package com.paymentproof.repository;

import com.paymentproof.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    Optional<AuditEvent> findTopByOrderBySequenceNumberDesc();
    List<AuditEvent> findAllByOrderBySequenceNumberAsc();
    List<AuditEvent> findByEntityIdOrderBySequenceNumberAsc(String entityId);
    List<AuditEvent> findByEntityIdOrderByCreatedAtAsc(String entityId);
    List<AuditEvent> findByEntityNameAndEntityIdOrderByCreatedAtAsc(String entityName, String entityId);
}
