package com.paymentproof.audit;

import com.paymentproof.dto.AuditVerificationResultDto;
import com.paymentproof.entity.AuditEvent;
import com.paymentproof.entity.enums.ActorType;
import com.paymentproof.repository.AuditEventRepository;
import com.paymentproof.service.AuditChainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditChainIntegrityTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditChainService auditChainService;

    private List<AuditEvent> inMemoryLedger;

    @BeforeEach
    void setUp() {
        inMemoryLedger = new ArrayList<>();
    }

    @Test
    @DisplayName("Empty ledger returns valid genesis state")
    void testEmptyLedgerVerification() {
        when(auditEventRepository.findAllByOrderBySequenceNumberAsc()).thenReturn(List.of());

        AuditVerificationResultDto result = auditChainService.verifyAuditChainIntegrity();

        assertTrue(result.isValid());
        assertEquals(0, result.getTotalEventsVerified());
        assertEquals(AuditChainService.GENESIS_PREV_HASH, result.getGenesisHash());
    }

    @Test
    @DisplayName("Valid chained audit events pass tamper-evident verification")
    void testValidChainedAuditTrail() {
        // Build block 1 (Genesis)
        String hash1 = AuditChainService.computeSha256(
                1L, "INCIDENT_CASES", "inc_001", "INVESTIGATION_STARTED", "WORKFLOW_ENGINE", "SYSTEM", "{}", "{\"status\":\"IN_PROGRESS\"}", AuditChainService.GENESIS_PREV_HASH
        );
        AuditEvent event1 = AuditEvent.builder()
                .auditId("aud_1")
                .sequenceNumber(1L)
                .entityName("INCIDENT_CASES")
                .entityId("inc_001")
                .action("INVESTIGATION_STARTED")
                .actorType(ActorType.WORKFLOW_ENGINE)
                .actorId("SYSTEM")
                .previousState("{}")
                .newState("{\"status\":\"IN_PROGRESS\"}")
                .previousEventHash(AuditChainService.GENESIS_PREV_HASH)
                .currentEventHash(hash1)
                .createdAt(LocalDateTime.now())
                .build();

        // Build block 2 (Linked to block 1)
        String hash2 = AuditChainService.computeSha256(
                2L, "INCIDENT_CASES", "inc_001", "ML_CLASSIFICATION_RECEIVED", "SYSTEM", "AI_CLASSIFIER", "{\"status\":\"IN_PROGRESS\"}", "{\"class\":\"BANK_DEBIT_GATEWAY_FAILURE\"}", hash1
        );
        AuditEvent event2 = AuditEvent.builder()
                .auditId("aud_2")
                .sequenceNumber(2L)
                .entityName("INCIDENT_CASES")
                .entityId("inc_001")
                .action("ML_CLASSIFICATION_RECEIVED")
                .actorType(ActorType.SYSTEM)
                .actorId("AI_CLASSIFIER")
                .previousState("{\"status\":\"IN_PROGRESS\"}")
                .newState("{\"class\":\"BANK_DEBIT_GATEWAY_FAILURE\"}")
                .previousEventHash(hash1)
                .currentEventHash(hash2)
                .createdAt(LocalDateTime.now())
                .build();

        // Build block 3 (Linked to block 2)
        String hash3 = AuditChainService.computeSha256(
                3L, "RESOLUTIONS", "res_001", "RESOLUTION_SELECTED", "OPERATOR_USER", "priya_m", "{\"status\":\"OPEN\"}", "{\"action\":\"AUTO_REFUND\"}", hash2
        );
        AuditEvent event3 = AuditEvent.builder()
                .auditId("aud_3")
                .sequenceNumber(3L)
                .entityName("RESOLUTIONS")
                .entityId("res_001")
                .action("RESOLUTION_SELECTED")
                .actorType(ActorType.OPERATOR_USER)
                .actorId("priya_m")
                .previousState("{\"status\":\"OPEN\"}")
                .newState("{\"action\":\"AUTO_REFUND\"}")
                .previousEventHash(hash2)
                .currentEventHash(hash3)
                .createdAt(LocalDateTime.now())
                .build();

        when(auditEventRepository.findAllByOrderBySequenceNumberAsc()).thenReturn(List.of(event1, event2, event3));

        AuditVerificationResultDto result = auditChainService.verifyAuditChainIntegrity();

        assertTrue(result.isValid());
        assertEquals(3, result.getTotalEventsVerified());
        assertEquals(hash3, result.getLatestHeadHash());
    }

    @Test
    @DisplayName("Tampering with a historical payload breaks SHA-256 verification and reports invalid chain")
    void testTamperDetectionOnModifiedPayload() {
        // Block 1
        String hash1 = AuditChainService.computeSha256(1L, "INCIDENT_CASES", "inc_001", "INVESTIGATION_STARTED", "SYSTEM", "SYS", "{}", "{}", AuditChainService.GENESIS_PREV_HASH);
        AuditEvent event1 = AuditEvent.builder()
                .auditId("aud_1").sequenceNumber(1L).entityName("INCIDENT_CASES").entityId("inc_001").action("INVESTIGATION_STARTED")
                .actorType(ActorType.SYSTEM).actorId("SYS").previousState("{}").newState("{}")
                .previousEventHash(AuditChainService.GENESIS_PREV_HASH).currentEventHash(hash1).createdAt(LocalDateTime.now()).build();

        // Block 2 with original hash
        String hash2 = AuditChainService.computeSha256(2L, "RESOLUTIONS", "res_001", "RESOLUTION_SELECTED", "OPERATOR_USER", "priya_m", "{}", "{\"action\":\"AUTO_REFUND\"}", hash1);
        
        // Simulating attacker tampering with newState to "CANCEL_REFUND" without valid hash
        AuditEvent tamperedEvent2 = AuditEvent.builder()
                .auditId("aud_2").sequenceNumber(2L).entityName("RESOLUTIONS").entityId("res_001").action("RESOLUTION_SELECTED")
                .actorType(ActorType.OPERATOR_USER).actorId("priya_m").previousState("{}").newState("{\"action\":\"CANCEL_REFUND_TAMPERED\"}") // TAMPERED
                .previousEventHash(hash1).currentEventHash(hash2).createdAt(LocalDateTime.now()).build();

        when(auditEventRepository.findAllByOrderBySequenceNumberAsc()).thenReturn(List.of(event1, tamperedEvent2));

        AuditVerificationResultDto result = auditChainService.verifyAuditChainIntegrity();

        assertFalse(result.isValid(), "Verification must report invalid chain when payload is altered");
        assertEquals("aud_2", result.getTamperedAuditId());
        assertEquals(2L, result.getTamperedAtSequence());
        assertTrue(result.getVerificationSummary().contains("Payload tampering detected"));
    }
}
