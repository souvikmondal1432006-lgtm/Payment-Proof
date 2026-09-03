package com.paymentproof.service;

import com.paymentproof.client.MlServiceClient;
import com.paymentproof.dto.*;
import com.paymentproof.entity.*;
import com.paymentproof.entity.enums.*;
import com.paymentproof.exception.ResourceNotFoundException;
import com.paymentproof.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestigationService {

    private static final BigDecimal ML_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.70);

    private final IncidentCaseRepository incidentCaseRepository;
    private final PaymentRepository paymentRepository;
    private final BankRecordRepository bankRecordRepository;
    private final GatewayRecordRepository gatewayRecordRepository;
    private final MerchantOrderRecordRepository merchantOrderRecordRepository;
    private final WebhookRecordRepository webhookRecordRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final InvestigationEvidenceRepository evidenceRepository;
    private final MlAssessmentRepository mlAssessmentRepository;
    private final AuditService auditService;
    private final MlServiceClient mlServiceClient;
    private final TimelineService timelineService;
    private final AiInvestigationService aiInvestigationService;

    @Transactional
    public InvestigationResultDto investigateIncident(String incidentId) {
        log.info("Starting authoritative forensic investigation for incident: {}", incidentId);

        IncidentCase incident = incidentCaseRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentCase", "incidentId", incidentId));

        Payment payment = paymentRepository.findById(incident.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", incident.getPaymentId()));

        BankRecord bank = bankRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        GatewayRecord gateway = gatewayRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        MerchantOrderRecord merchantOrder = merchantOrderRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        WebhookRecord webhook = webhookRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        SettlementRecord settlement = settlementRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        RefundRecord refund = refundRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);

        // 1. Detect Contradictions Across Multi-Party Records (Deterministic Ground Truth)
        List<String> contradictions = new ArrayList<>();
        boolean isBankDebited = (bank != null && (bank.getBankStatus() == BankStatus.SUCCESS || bank.getBankStatus() == BankStatus.DEBITED));
        boolean isGatewayCaptured = (gateway != null && gateway.getCaptureStatus() == CaptureStatus.CAPTURED);
        boolean isGatewayFailed = (gateway != null && (gateway.getGatewayStatus() == GatewayStatus.FAILED || gateway.getGatewayStatus() == GatewayStatus.TIMED_OUT));
        boolean isMerchantPaid = (merchantOrder != null && merchantOrder.getOrderStatus() == OrderStatus.PAID);
        boolean isMerchantCancelled = (merchantOrder != null && merchantOrder.getOrderStatus() == OrderStatus.CANCELLED);
        boolean isWebhookFailed = (webhook != null && (webhook.getDeliveryStatus() == WebhookDeliveryStatus.FAILED || webhook.getDeliveryStatus() == WebhookDeliveryStatus.DROPPED));

        if (isBankDebited && isGatewayFailed) {
            contradictions.add(String.format("Ghost Debit: Bank debited INR %s (UTR: %s, status: %s), but Gateway reported %s (error: %s).",
                    payment.getAmount(), bank.getUtrNumber(), bank.getBankStatus(), gateway.getGatewayStatus(), gateway.getErrorCode()));
        }

        if (isBankDebited && isMerchantCancelled) {
            contradictions.add(String.format("Cart Cancellation Disconnect: Customer was debited INR %s at Bank, but Merchant marked order %s as CANCELLED (%s).",
                    payment.getAmount(), merchantOrder.getMerchantOrderId(), merchantOrder.getCancellationReason()));
        }

        if (isGatewayCaptured && isWebhookFailed) {
            contradictions.add(String.format("Dropped Webhook: Gateway captured funds, but Webhook delivery failed with HTTP %s after %d attempts to %s.",
                    webhook.getHttpStatusCode(), webhook.getAttemptCount(), webhook.getTargetUrl()));
        }

        if (isGatewayCaptured && !isMerchantPaid) {
            contradictions.add(String.format("Unfulfilled Captured Payment: Gateway successfully captured INR %s, but Merchant OMS status is %s (fulfillment: %s).",
                    payment.getAmount(), merchantOrder != null ? merchantOrder.getOrderStatus() : "UNKNOWN",
                    merchantOrder != null ? merchantOrder.getFulfillmentStatus() : "UNKNOWN"));
        }

        if (settlement != null && settlement.getSettlementStatus() == SettlementStatus.DISCREPANCY) {
            BigDecimal expectedNet = settlement.getGrossAmount().subtract(settlement.getFeeDeducted()).subtract(settlement.getTaxDeducted());
            BigDecimal variance = expectedNet.subtract(settlement.getNetSettledAmount());
            contradictions.add(String.format("Settlement Ledger Variance: Expected net settlement INR %s, but actual settled is INR %s (variance: INR %s).",
                    expectedNet, settlement.getNetSettledAmount(), variance));
        }

        if (refund != null && refund.getRefundStatus() == RefundStatus.MANUAL_INTERVENTION_REQUIRED) {
            contradictions.add(String.format("Refund Desynchronization: Gateway processed refund for ARN %s, but Bank reversal network reported status: %s.",
                    refund.getRefundArn(), refund.getBankReversalStatus()));
        }

        // 2. Financial Safety Invariants: Money Lock
        boolean isRetryProhibited = false;
        String retryReason = null;

        if (isBankDebited) {
            isRetryProhibited = true;
            retryReason = String.format("STRICT SAFETY INVARIANT: Active bank debit confirmed with UTR %s. Blind retry is prohibited to prevent double charging.",
                    bank.getUtrNumber());
        } else if (isGatewayCaptured) {
            isRetryProhibited = true;
            retryReason = "STRICT SAFETY INVARIANT: Gateway funds are actively captured. Blind retry is prohibited to prevent double charging.";
        }

        BigDecimal moneyAtRisk = isBankDebited ? payment.getAmount() : BigDecimal.ZERO;

        // 3. Duplicate Investigation Idempotency Check
        Optional<MlAssessment> existingAssessment = mlAssessmentRepository.findByIncidentId(incident.getIncidentId());
        if (incident.getCaseStatus() == CaseStatus.AI_ANALYZED && existingAssessment.isPresent()) {
            log.info("Incident {} was already investigated. Returning idempotent result.", incidentId);
            auditService.logEvent(
                    "INCIDENT_CASES",
                    incident.getIncidentId(),
                    "OPERATOR_REVIEWED_CASE",
                    ActorType.OPERATOR_USER,
                    "WORKFLOW_IDEMPOTENT_READER",
                    String.format("{\"status\":\"%s\"}", incident.getCaseStatus()),
                    String.format("{\"status\":\"%s\",\"idempotent\":true}", incident.getCaseStatus()),
                    "127.0.0.1"
            );
            return buildInvestigationResult(incident, payment, bank, gateway, merchantOrder, webhook, settlement, refund,
                    existingAssessment.map(this::mapAssessmentToDto), contradictions, isRetryProhibited, retryReason, moneyAtRisk,
                    existingAssessment.get().getPredictedRootCause(), existingAssessment.get().getConfidenceScore(),
                    existingAssessment.get().getSuggestedAction(), incident.getCaseStatus(), "Existing investigation result returned via idempotent lookup.");
        }

        // 4. Extract Features for ML Classification
        MlFeatureRequestDto featureRequest = MlFeatureRequestDto.builder()
                .paymentId(payment.getPaymentId())
                .incidentId(incident.getIncidentId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "UPI")
                .bankName(bank != null ? bank.getBankName() : null)
                .bankStatus(bank != null && bank.getBankStatus() != null ? bank.getBankStatus().name() : null)
                .bankLatencyMs(bank != null ? bank.getNetworkLatencyMs() : null)
                .gatewayName(gateway != null ? gateway.getGatewayName() : null)
                .gatewayStatus(gateway != null && gateway.getGatewayStatus() != null ? gateway.getGatewayStatus().name() : null)
                .gatewayAuthStatus(gateway != null && gateway.getAuthStatus() != null ? gateway.getAuthStatus().name() : null)
                .gatewayCaptureStatus(gateway != null && gateway.getCaptureStatus() != null ? gateway.getCaptureStatus().name() : null)
                .gatewayLatencyMs(gateway != null ? gateway.getProcessingLatencyMs() : null)
                .merchantStatus(merchantOrder != null && merchantOrder.getOrderStatus() != null ? merchantOrder.getOrderStatus().name() : null)
                .merchantFulfillment(merchantOrder != null && merchantOrder.getFulfillmentStatus() != null ? merchantOrder.getFulfillmentStatus().name() : null)
                .webhookStatus(webhook != null && webhook.getDeliveryStatus() != null ? webhook.getDeliveryStatus().name() : null)
                .webhookHttpCode(webhook != null ? webhook.getHttpStatusCode() : null)
                .webhookAttempts(webhook != null ? webhook.getAttemptCount() : null)
                .isAmountMatched(bank != null && bank.getDebitedAmount() != null && bank.getDebitedAmount().compareTo(payment.getAmount()) == 0)
                .build();

        // 5. Query Python ML Service (with strict rejection of malformed or invalid confidence responses)
        Optional<MlAssessmentDto> mlAssessmentOpt = mlServiceClient.classifyTelemetry(featureRequest);

        // 6. Combine ML Output with Deterministic Evidence
        CaseStatus finalCaseStatus;
        SuggestedAction recommendedAction;
        String predictedRootCause;
        BigDecimal confidence;
        BigDecimal anomalyScore;
        String modelVersion;
        String modelExplanation;
        String summary;

        if (mlAssessmentOpt.isEmpty()) {
            // PHASE 10: ML Failure / Timeout / Malformed -> Safe Hold / Needs Review, ZERO fake prediction
            finalCaseStatus = CaseStatus.NEEDS_REVIEW;
            predictedRootCause = "UNAVAILABLE";
            confidence = null; // Zero synthetic confidence
            anomalyScore = null;
            modelVersion = "UNAVAILABLE";
            modelExplanation = "AI prediction unavailable — multi-party evidence is still available. No automatic payment action was taken.";
            summary = "Automated classification unavailable. Evidence remains available for manual investigation.";

            if (isBankDebited && (isGatewayFailed || isMerchantCancelled)) {
                recommendedAction = SuggestedAction.AUTO_REFUND_CUSTOMER;
            } else if (isGatewayCaptured && isWebhookFailed) {
                recommendedAction = SuggestedAction.RESEND_WEBHOOK;
            } else if (settlement != null && settlement.getSettlementStatus() == SettlementStatus.DISCREPANCY) {
                recommendedAction = SuggestedAction.FORCE_SETTLE_MERCHANT;
            } else {
                recommendedAction = SuggestedAction.MANUAL_BANK_ESCALATION;
            }
        } else {
            MlAssessmentDto ml = mlAssessmentOpt.get();
            predictedRootCause = ml.getEffectivePredictedClass();
            confidence = ml.getEffectiveConfidence();
            anomalyScore = ml.getAnomalyScore();
            modelVersion = ml.getModelVersion() != null ? ml.getModelVersion() : "incident-classifier-v1.0.0-rf";
            modelExplanation = ml.getModelExplanation();

            MlAssessment assessmentEntity = mlAssessmentRepository.findByIncidentId(incident.getIncidentId())
                    .orElseGet(() -> MlAssessment.builder()
                            .assessmentId("mla_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                            .incidentId(incident.getIncidentId())
                            .paymentId(payment.getPaymentId())
                            .build());

            assessmentEntity.setModelVersion(modelVersion);
            assessmentEntity.setPredictedRootCause(predictedRootCause);
            assessmentEntity.setAnomalyScore(anomalyScore);
            assessmentEntity.setConfidenceScore(confidence);
            
            if (ml.getSuggestedAction() != null) {
                assessmentEntity.setSuggestedAction(ml.getSuggestedAction());
            } else if (ml.getRecommendedAction() != null) {
                try {
                    assessmentEntity.setSuggestedAction(SuggestedAction.valueOf(ml.getRecommendedAction()));
                } catch (IllegalArgumentException e) {
                    assessmentEntity.setSuggestedAction(SuggestedAction.MANUAL_BANK_ESCALATION);
                }
            }
            assessmentEntity.setModelExplanation(modelExplanation);
            assessmentEntity.setAssessedAt(LocalDateTime.now());
            mlAssessmentRepository.save(assessmentEntity);

            if (confidence != null && confidence.compareTo(ML_CONFIDENCE_THRESHOLD) < 0) {
                finalCaseStatus = CaseStatus.NEEDS_REVIEW;
                recommendedAction = assessmentEntity.getSuggestedAction() != null ? assessmentEntity.getSuggestedAction() : SuggestedAction.MANUAL_BANK_ESCALATION;
                summary = String.format("Automated ML prediction confidence (%s%%) is below threshold (70.0%%). Root cause suggested as '%s'. Escalated to operator review.",
                        confidence.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP), predictedRootCause);
            } else if (isBankDebited && isMerchantCancelled && !"BANK_DEBIT_GATEWAY_FAILURE".equalsIgnoreCase(predictedRootCause) && !"ORDER_PAYMENT_CONFLICT".equalsIgnoreCase(predictedRootCause)) {
                finalCaseStatus = CaseStatus.AI_ANALYZED;
                recommendedAction = SuggestedAction.AUTO_REFUND_CUSTOMER;
                isRetryProhibited = true;
                summary = String.format("Investigation concluded: ML model predicted '%s' (%s%% confidence), but deterministic multi-party telemetry confirms active bank debit with cancelled merchant order. Overridden to auto-refund customer.",
                        predictedRootCause, confidence.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
            } else {
                finalCaseStatus = CaseStatus.AI_ANALYZED;
                recommendedAction = assessmentEntity.getSuggestedAction() != null ? assessmentEntity.getSuggestedAction() : SuggestedAction.MANUAL_BANK_ESCALATION;
                summary = String.format("Investigation concluded for %s: Root cause predicted as '%s' with %s confidence. Prohibit retry: %b.",
                        incident.getIncidentId(), predictedRootCause, confidence, isRetryProhibited);
            }
        }

        // 7. Update Incident Case Status in MySQL
        incident.setCaseStatus(finalCaseStatus);
        incidentCaseRepository.save(incident);

        // 8. Record Chained Tamper-Evident Audit Event
        auditService.logEvent(
                "INCIDENT_CASES",
                incident.getIncidentId(),
                mlAssessmentOpt.isPresent() ? "INVESTIGATION_COMPLETED" : "INVESTIGATION_COMPLETED_ML_OFFLINE",
                ActorType.WORKFLOW_ENGINE,
                "JAVA_AI_INVESTIGATOR",
                String.format("{\"status\":\"%s\"}", CaseStatus.OPEN),
                String.format("{\"status\":\"%s\",\"root_cause\":\"%s\",\"action\":\"%s\"}",
                        finalCaseStatus, predictedRootCause, recommendedAction),
                "127.0.0.1"
        );

        return buildInvestigationResult(incident, payment, bank, gateway, merchantOrder, webhook, settlement, refund,
                mlAssessmentOpt, contradictions, isRetryProhibited, retryReason, moneyAtRisk,
                predictedRootCause, confidence, recommendedAction, finalCaseStatus, summary);
    }

    private InvestigationResultDto buildInvestigationResult(
            IncidentCase incident, Payment payment, BankRecord bank, GatewayRecord gateway,
            MerchantOrderRecord merchantOrder, WebhookRecord webhook, SettlementRecord settlement,
            RefundRecord refund, Optional<MlAssessmentDto> mlAssessmentOpt, List<String> contradictions,
            boolean isRetryProhibited, String retryReason, BigDecimal moneyAtRisk,
            String predictedRootCause, BigDecimal confidence, SuggestedAction recommendedAction,
            CaseStatus finalCaseStatus, String summary) {

        List<InvestigationEvidence> evidenceList = evidenceRepository.findByIncidentId(incident.getIncidentId());
        List<InvestigationEvidenceDto> evidenceDtos = evidenceList.stream()
                .map(e -> InvestigationEvidenceDto.builder()
                        .evidenceId(e.getEvidenceId())
                        .incidentId(e.getIncidentId())
                        .paymentId(e.getPaymentId())
                        .evidenceSource(e.getEvidenceSource())
                        .evidenceType(e.getEvidenceType())
                        .filePath(e.getFilePath())
                        .rawContent(e.getRawContent())
                        .payloadChecksum(e.getPayloadChecksum())
                        .capturedAt(e.getCapturedAt())
                        .build())
                .collect(Collectors.toList());

        List<TimelineEventDto> timelineEvents = timelineService.getTimelineForPayment(payment.getPaymentId());

        AiInvestigationReportDto aiReport = aiInvestigationService.generateInvestigationReport(
                incident, payment, bank, gateway, merchantOrder, webhook, settlement, refund,
                evidenceDtos, timelineEvents, mlAssessmentOpt, contradictions, isRetryProhibited,
                retryReason, moneyAtRisk
        );

        return InvestigationResultDto.builder()
                .incidentId(incident.getIncidentId())
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .merchantId(payment.getMerchantId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "UPI")
                .incidentClassification(incident.getIncidentType())
                .severity(incident.getSeverity())
                .investigationStatus(finalCaseStatus)
                .predictedRootCause(predictedRootCause)
                .confidence(confidence)
                .recommendedAction(recommendedAction)
                .isRetryProhibited(isRetryProhibited)
                .retryProhibitionReason(retryReason)
                .moneyAtRisk(moneyAtRisk)
                .contradictionsDetected(contradictions)
                .evidenceList(evidenceDtos)
                .evidenceCount(evidenceDtos.size())
                .aiReport(aiReport)
                .summary(summary)
                .investigatedAt(LocalDateTime.now())
                .build();
    }

    private MlAssessmentDto mapAssessmentToDto(MlAssessment a) {
        if (a == null) return null;
        return MlAssessmentDto.builder()
                .assessmentId(a.getAssessmentId())
                .incidentId(a.getIncidentId())
                .paymentId(a.getPaymentId())
                .modelVersion(a.getModelVersion())
                .predictedRootCause(a.getPredictedRootCause())
                .anomalyScore(a.getAnomalyScore())
                .confidenceScore(a.getConfidenceScore())
                .suggestedAction(a.getSuggestedAction())
                .modelExplanation(a.getModelExplanation())
                .assessedAt(a.getAssessedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public AiInvestigationReportDto getAiReportForIncident(String incidentId) {
        IncidentCase incident = incidentCaseRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentCase", "incidentId", incidentId));

        Payment payment = paymentRepository.findById(incident.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", incident.getPaymentId()));

        BankRecord bank = bankRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        GatewayRecord gateway = gatewayRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        MerchantOrderRecord merchantOrder = merchantOrderRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        WebhookRecord webhook = webhookRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        SettlementRecord settlement = settlementRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        RefundRecord refund = refundRecordRepository.findByPaymentId(payment.getPaymentId()).orElse(null);
        List<InvestigationEvidence> evidenceList = evidenceRepository.findByIncidentId(incident.getIncidentId());

        List<InvestigationEvidenceDto> evidenceDtos = evidenceList.stream()
                .map(e -> InvestigationEvidenceDto.builder()
                        .evidenceId(e.getEvidenceId())
                        .incidentId(e.getIncidentId())
                        .paymentId(e.getPaymentId())
                        .evidenceSource(e.getEvidenceSource())
                        .evidenceType(e.getEvidenceType())
                        .filePath(e.getFilePath())
                        .rawContent(e.getRawContent())
                        .payloadChecksum(e.getPayloadChecksum())
                        .capturedAt(e.getCapturedAt())
                        .build())
                .collect(Collectors.toList());

        List<TimelineEventDto> timelineEvents = timelineService.getTimelineForPayment(payment.getPaymentId());
        Optional<MlAssessment> assessment = mlAssessmentRepository.findByIncidentId(incidentId);

        boolean isBankDebited = (bank != null && (bank.getBankStatus() == BankStatus.SUCCESS || bank.getBankStatus() == BankStatus.DEBITED));
        boolean isRetryProhibited = isBankDebited;
        String retryReason = isBankDebited ? "Active bank debit confirmed." : null;
        BigDecimal moneyAtRisk = isBankDebited ? payment.getAmount() : BigDecimal.ZERO;

        return aiInvestigationService.generateInvestigationReport(
                incident, payment, bank, gateway, merchantOrder, webhook, settlement, refund,
                evidenceDtos, timelineEvents, assessment.map(this::mapAssessmentToDto),
                Collections.emptyList(), isRetryProhibited, retryReason, moneyAtRisk
        );
    }
}
