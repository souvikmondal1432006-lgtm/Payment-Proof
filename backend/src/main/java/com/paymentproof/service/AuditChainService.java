package com.paymentproof.service;

import com.paymentproof.dto.AuditVerificationResultDto;
import com.paymentproof.entity.AuditEvent;
import com.paymentproof.entity.enums.ActorType;
import com.paymentproof.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditChainService {

    public static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditEventRepository auditEventRepository;

    /**
     * Records an authoritative audit event linking to the previous hash in the sequence chain.
     */
    @Transactional
    public synchronized AuditEvent recordChainedEvent(
            String entityName,
            String entityId,
            String action,
            ActorType actorType,
            String actorId,
            String previousState,
            String newState,
            String ipAddress) {

        Optional<AuditEvent> latestOpt = auditEventRepository.findTopByOrderBySequenceNumberDesc();

        long sequence = latestOpt.map(a -> a.getSequenceNumber() + 1).orElse(1L);
        String prevHash = latestOpt.map(AuditEvent::getCurrentEventHash).orElse(GENESIS_PREV_HASH);

        String currentHash = computeSha256(
                sequence,
                entityName,
                entityId,
                action,
                actorType != null ? actorType.name() : "SYSTEM",
                actorId,
                previousState,
                newState,
                prevHash
        );

        AuditEvent event = AuditEvent.builder()
                .auditId("aud_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .sequenceNumber(sequence)
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .actorType(actorType != null ? actorType : ActorType.SYSTEM)
                .actorId(actorId != null ? actorId : "SYSTEM")
                .previousState(previousState)
                .newState(newState)
                .ipAddress(ipAddress != null ? ipAddress : "127.0.0.1")
                .previousEventHash(prevHash)
                .currentEventHash(currentHash)
                .build();

        AuditEvent saved = auditEventRepository.save(event);
        log.info("Recorded chained audit event [seq: {}, action: {}, id: {}] with hash: {}",
                sequence, action, saved.getAuditId(), currentHash);

        return saved;
    }

    /**
     * Recomputes and validates the entire cryptographic SHA-256 audit chain from genesis to head.
     * Accurately describes the result as "tamper-evident audit logging".
     */
    @Transactional(readOnly = true)
    public AuditVerificationResultDto verifyAuditChainIntegrity() {
        List<AuditEvent> events = auditEventRepository.findAllByOrderBySequenceNumberAsc();

        if (events.isEmpty()) {
            return AuditVerificationResultDto.builder()
                    .isValid(true)
                    .totalEventsVerified(0)
                    .genesisHash(GENESIS_PREV_HASH)
                    .latestHeadHash(GENESIS_PREV_HASH)
                    .verificationSummary("Audit ledger is empty. Genesis state initialized.")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        String expectedPrevHash = GENESIS_PREV_HASH;
        long expectedSequence = 1L;

        for (AuditEvent event : events) {
            // 1. Check sequence ordering
            if (event.getSequenceNumber() == null || event.getSequenceNumber() != expectedSequence) {
                return AuditVerificationResultDto.builder()
                        .isValid(false)
                        .totalEventsVerified(events.size())
                        .tamperedAuditId(event.getAuditId())
                        .tamperedAtSequence(event.getSequenceNumber())
                        .expectedHash("Sequence " + expectedSequence)
                        .actualHash("Found " + event.getSequenceNumber())
                        .verificationSummary("Sequence discontinuity detected at sequence " + event.getSequenceNumber())
                        .verifiedAt(LocalDateTime.now())
                        .build();
            }

            // 2. Check previous hash link
            if (!expectedPrevHash.equals(event.getPreviousEventHash())) {
                return AuditVerificationResultDto.builder()
                        .isValid(false)
                        .totalEventsVerified(events.size())
                        .tamperedAuditId(event.getAuditId())
                        .tamperedAtSequence(event.getSequenceNumber())
                        .expectedHash(expectedPrevHash)
                        .actualHash(event.getPreviousEventHash())
                        .verificationSummary("Cryptographic chain broken at sequence " + event.getSequenceNumber() + ". Previous event hash does not match prior record.")
                        .verifiedAt(LocalDateTime.now())
                        .build();
            }

            // 3. Recompute and check current event hash
            String recomputedHash = computeSha256(
                    event.getSequenceNumber(),
                    event.getEntityName(),
                    event.getEntityId(),
                    event.getAction(),
                    event.getActorType() != null ? event.getActorType().name() : "SYSTEM",
                    event.getActorId(),
                    event.getPreviousState(),
                    event.getNewState(),
                    event.getPreviousEventHash()
            );

            if (!recomputedHash.equals(event.getCurrentEventHash())) {
                return AuditVerificationResultDto.builder()
                        .isValid(false)
                        .totalEventsVerified(events.size())
                        .tamperedAuditId(event.getAuditId())
                        .tamperedAtSequence(event.getSequenceNumber())
                        .expectedHash(recomputedHash)
                        .actualHash(event.getCurrentEventHash())
                        .verificationSummary("Payload tampering detected at sequence " + event.getSequenceNumber() + " (Audit ID: " + event.getAuditId() + "). Stored hash does not match payload digest.")
                        .verifiedAt(LocalDateTime.now())
                        .build();
            }

            expectedPrevHash = event.getCurrentEventHash();
            expectedSequence++;
        }

        String headHash = events.get(events.size() - 1).getCurrentEventHash();

        return AuditVerificationResultDto.builder()
                .isValid(true)
                .totalEventsVerified(events.size())
                .genesisHash(GENESIS_PREV_HASH)
                .latestHeadHash(headHash)
                .verificationSummary("Tamper-evident verification successful: All " + events.size() + " audit records verified unbroken under SHA-256 cryptographic chain.")
                .verifiedAt(LocalDateTime.now())
                .build();
    }

    public static String computeSha256(
            Long sequence,
            String entityName,
            String entityId,
            String action,
            String actorType,
            String actorId,
            String previousState,
            String newState,
            String previousHash) {

        String raw = String.format("%d|%s|%s|%s|%s|%s|%s|%s|%s",
                sequence != null ? sequence : 0L,
                entityName != null ? entityName : "",
                entityId != null ? entityId : "",
                action != null ? action : "",
                actorType != null ? actorType : "",
                actorId != null ? actorId : "",
                previousState != null ? previousState : "",
                newState != null ? newState : "",
                previousHash != null ? previousHash : ""
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
