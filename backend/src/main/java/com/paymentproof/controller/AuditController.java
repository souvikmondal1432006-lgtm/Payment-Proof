package com.paymentproof.controller;

import com.paymentproof.dto.AuditEventDto;
import com.paymentproof.dto.AuditVerificationResultDto;
import com.paymentproof.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditEventDto>> getAllAuditEvents() {
        log.info("Fetching complete audit ledger");
        List<AuditEventDto> auditEvents = auditService.getAllAuditEvents();
        return ResponseEntity.ok(auditEvents);
    }

    @GetMapping("/verify")
    public ResponseEntity<AuditVerificationResultDto> verifyAuditLedger() {
        log.info("Executing authoritative cryptographic verification on SHA-256 audit chain");
        AuditVerificationResultDto result = auditService.verifyAuditLedgerIntegrity();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<AuditEventDto>> getAuditTrailForEntity(@PathVariable("entityId") String entityId) {
        log.info("Fetching audit trail for entity ID: {}", entityId);
        List<AuditEventDto> auditEvents = auditService.getAuditTrailForEntity(entityId);
        return ResponseEntity.ok(auditEvents);
    }
}
