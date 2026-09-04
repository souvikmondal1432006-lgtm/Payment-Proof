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
    private final GeminiInvestigationService geminiInvestigationService;

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

        if (isBankDebited && isGatewayCaptured && isMerchantCancelled) {
            contradictions.add(String.format("Payment Capture with Cancelled Order: Bank (UTR: %s) and Gateway confirmed capture of INR %s, but Merchant marked order %s as CANCELLED (%s).",
                    bank.getUtrNumber(), payment.getAmount(), merchantOrder.getMerchantOrderId(), merchantOrder.getCancellationReason()));
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
        if (incident.getCaseStatus() == CaseStatus.RESOLVED ||
                ((incident.getCaseStatus() == CaseStatus.AI_ANALYZED || incident.getCaseStatus() == CaseStatus.NEEDS_REVIEW) && existingAssessment.isPresent())) {
            log.info("Incident {} was already investigated or resolved (status: {}). Returning idempotent result.", incidentId, incident.getCaseStatus());
            return buildInvestigationResult(incident, payment, bank, gateway, merchantOrder, webhook, settlement, refund,
                    existingAssessment.map(this::mapAssessmentToDto), contradictions, isRetryProhibited, retryReason, moneyAtRisk,
                    existingAssessment.map(MlAssessment::getPredictedRootCause).orElse("RESOLVED"),
                    existingAssessment.map(MlAssessment::getConfidenceScore).orElse(null),
                    existingAssessment.map(MlAssessment::getSuggestedAction).orElse(SuggestedAction.NO_ACTION_REQUIRED),
                    incident.getCaseStatus(), "Existing investigation result returned via idempotent lookup.");
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
            assessmentEntity.setModelExplanation(modelExplanation);
            assessmentEntity.setAssessedAt(LocalDateTime.now());

            // JAVA AUTHORITATIVE SAFETY & ACTION DETERMINATION
            // Random Forest ML classifies telemetry features ONLY.
            // Java deterministic safety rules calculate ALL operational and financial decisions.
            recommendedAction = determineJavaSafetyAction(
                    predictedRootCause,
                    confidence,
                    isBankDebited,
                    isGatewayCaptured,
                    isGatewayFailed,
                    isMerchantPaid,
                    isMerchantCancelled,
                    isWebhookFailed,
                    settlement,
                    refund
            );

            // Record Java's authoritative action decision in the assessment entity
            assessmentEntity.setSuggestedAction(recommendedAction);
            mlAssessmentRepository.save(assessmentEntity);

            if (confidence != null && confidence.compareTo(ML_CONFIDENCE_THRESHOLD) < 0) {
                finalCaseStatus = CaseStatus.NEEDS_REVIEW;
                summary = String.format("Automated ML prediction confidence (%s%%) is below threshold (70.0%%). Root cause suggested as '%s'. Java safety decision: %s. Escalated to operator review.",
                        confidence.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP), predictedRootCause, recommendedAction);
            } else if (isBankDebited && isMerchantCancelled && !"BANK_DEBIT_GATEWAY_FAILURE".equalsIgnoreCase(predictedRootCause) && !"ORDER_PAYMENT_CONFLICT".equalsIgnoreCase(predictedRootCause)) {
                finalCaseStatus = CaseStatus.AI_ANALYZED;
                summary = String.format("Investigation concluded: ML model predicted '%s' (%s%% confidence), but deterministic multi-party telemetry confirms active bank debit with cancelled merchant order. Overridden to auto-refund customer.",
                        predictedRootCause, confidence.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
            } else {
                finalCaseStatus = CaseStatus.AI_ANALYZED;
                summary = String.format("Investigation concluded for %s: Root cause predicted as '%s' with %s confidence. Authoritative action: %s. Prohibit retry: %b.",
                        incident.getIncidentId(), predictedRootCause, confidence, recommendedAction, isRetryProhibited);
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

        // 9. Gemini Investigation Assistant (Strictly Advisory - Explanations only)
        List<TimelineEventDto> timelineEvents = timelineService.getTimelineForPayment(payment.getPaymentId());
        List<String> timelineSummary = timelineEvents.stream()
                .map(t -> String.format("[%s] %s: %s", t.getTimestamp(), t.getEventType(), t.getDescription()))
                .limit(10)
                .collect(Collectors.toList());

        GeminiPromptPayloadDto geminiPayload = GeminiPromptPayloadDto.builder()
                // 1. PAYMENT FACTS
                .paymentId(payment.getPaymentId())
                .incidentId(incident.getIncidentId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "UPI")
                // 2. MULTI-PARTY EVIDENCE & TIMESTAMPS
                .bankName(bank != null ? bank.getBankName() : null)
                .bankStatus(bank != null && bank.getBankStatus() != null ? bank.getBankStatus().name() : null)
                .bankUtr(bank != null ? bank.getUtrNumber() : null)
                .bankLatencyMs(bank != null ? bank.getNetworkLatencyMs() : null)
                .bankTimestamp(bank != null && bank.getBankTimestamp() != null ? bank.getBankTimestamp().toString() : (bank != null && bank.getCreatedAt() != null ? bank.getCreatedAt().toString() : null))
                .gatewayName(gateway != null ? gateway.getGatewayName() : null)
                .gatewayStatus(gateway != null && gateway.getGatewayStatus() != null ? gateway.getGatewayStatus().name() : null)
                .gatewayAuthStatus(gateway != null && gateway.getAuthStatus() != null ? gateway.getAuthStatus().name() : null)
                .gatewayCaptureStatus(gateway != null && gateway.getCaptureStatus() != null ? gateway.getCaptureStatus().name() : null)
                .gatewayLatencyMs(gateway != null ? gateway.getProcessingLatencyMs() : null)
                .gatewayTimestamp(gateway != null && gateway.getGatewayTimestamp() != null ? gateway.getGatewayTimestamp().toString() : (gateway != null && gateway.getCreatedAt() != null ? gateway.getCreatedAt().toString() : null))
                .merchantOrderStatus(merchantOrder != null && merchantOrder.getOrderStatus() != null ? merchantOrder.getOrderStatus().name() : null)
                .merchantFulfillmentStatus(merchantOrder != null && merchantOrder.getFulfillmentStatus() != null ? merchantOrder.getFulfillmentStatus().name() : null)
                .merchantTimestamp(merchantOrder != null && merchantOrder.getMerchantUpdatedAt() != null ? merchantOrder.getMerchantUpdatedAt().toString() : null)
                .webhookStatus(webhook != null && webhook.getDeliveryStatus() != null ? webhook.getDeliveryStatus().name() : null)
                .webhookHttpCode(webhook != null ? webhook.getHttpStatusCode() : null)
                .webhookAttemptCount(webhook != null ? webhook.getAttemptCount() : null)
                .webhookTimestamp(webhook != null && webhook.getLastAttemptAt() != null ? webhook.getLastAttemptAt().toString() : (webhook != null && webhook.getFirstAttemptAt() != null ? webhook.getFirstAttemptAt().toString() : null))
                .settlementStatus(settlement != null && settlement.getSettlementStatus() != null ? settlement.getSettlementStatus().name() : null)
                .refundStatus(refund != null && refund.getRefundStatus() != null ? refund.getRefundStatus().name() : null)
                // 3. JAVA INVESTIGATION (Deterministic Ground Truth)
                .detectedContradictions(contradictions)
                .deterministicFindings(summary)
                .relevantDomainSignals(contradictions)
                .investigationTimelineSummary(timelineSummary)
                // 4. RANDOM FOREST ML RESULT
                .mlModelName("Random Forest Classifier (v1.0.0)")
                .mlPredictedClass(predictedRootCause)
                .mlConfidence(confidence)
                .topMlSignals(mlAssessmentOpt.map(MlAssessmentDto::getTopContributingSignals).orElse(Collections.emptyList()))
                // 5. JAVA SAFETY DECISION (Authoritative & Final)
                .javaSafetyDecision(isRetryProhibited ? "PROHIBIT_RETRY_ACTIVE_FUNDS" : "RETRY_PERMITTED")
                .isRetryProhibited(isRetryProhibited)
                .retryProhibitionReason(retryReason)
                .moneyAtRisk(moneyAtRisk)
                .isAutomaticActionAllowed(finalCaseStatus == CaseStatus.AI_ANALYZED && (recommendedAction == SuggestedAction.AUTO_REFUND_CUSTOMER || recommendedAction == SuggestedAction.RESEND_WEBHOOK))
                .recommendedResolution(recommendedAction != null ? recommendedAction.name() : "MANUAL_BANK_ESCALATION")
                .build();

        Optional<GeminiInvestigationResponseDto> geminiExplanationOpt = geminiInvestigationService.explainInvestigation(geminiPayload);

        if (geminiExplanationOpt.isPresent()) {
            auditService.logEvent(
                    "INCIDENT_CASES",
                    incident.getIncidentId(),
                    "GEMINI_EXPLANATION_ATTACHED",
                    ActorType.SYSTEM,
                    "GEMINI_EXPLANATION_ASSISTANT",
                    String.format("{\"caseStatus\":\"%s\",\"investigatedBy\":\"JAVA_FORENSIC_ENGINE\"}", finalCaseStatus),
                    String.format("{\"role\":\"ADVISORY_EXPLANATION_ONLY\",\"financialAuthority\":\"JAVA_DETERMINISTIC_RULES\",\"approvedAction\":\"%s\",\"modelUsed\":\"%s\"}",
                            recommendedAction, geminiExplanationOpt.get().getModelUsed()),
                    "127.0.0.1"
            );

            // Persist explanation in MlAssessment if entity exists
            mlAssessmentRepository.findByIncidentId(incident.getIncidentId()).ifPresent(assessment -> {
                assessment.setGeminiExplanation(geminiExplanationOpt.get().getSummary());
                mlAssessmentRepository.save(assessment);
            });
        }

        return buildInvestigationResult(incident, payment, bank, gateway, merchantOrder, webhook, settlement, refund,
                mlAssessmentOpt, contradictions, isRetryProhibited, retryReason, moneyAtRisk,
                predictedRootCause, confidence, recommendedAction, finalCaseStatus, summary, geminiExplanationOpt);
    }

    private InvestigationResultDto buildInvestigationResult(
            IncidentCase incident, Payment payment, BankRecord bank, GatewayRecord gateway,
            MerchantOrderRecord merchantOrder, WebhookRecord webhook, SettlementRecord settlement,
            RefundRecord refund, Optional<MlAssessmentDto> mlAssessmentOpt, List<String> contradictions,
            boolean isRetryProhibited, String retryReason, BigDecimal moneyAtRisk,
            String predictedRootCause, BigDecimal confidence, SuggestedAction recommendedAction,
            CaseStatus finalCaseStatus, String summary) {
        return buildInvestigationResult(incident, payment, bank, gateway, merchantOrder, webhook,
                settlement, refund, mlAssessmentOpt, contradictions, isRetryProhibited, retryReason,
                moneyAtRisk, predictedRootCause, confidence, recommendedAction, finalCaseStatus, summary, Optional.empty());
    }

    private InvestigationResultDto buildInvestigationResult(
            IncidentCase incident, Payment payment, BankRecord bank, GatewayRecord gateway,
            MerchantOrderRecord merchantOrder, WebhookRecord webhook, SettlementRecord settlement,
            RefundRecord refund, Optional<MlAssessmentDto> mlAssessmentOpt, List<String> contradictions,
            boolean isRetryProhibited, String retryReason, BigDecimal moneyAtRisk,
            String predictedRootCause, BigDecimal confidence, SuggestedAction recommendedAction,
            CaseStatus finalCaseStatus, String summary,
            Optional<GeminiInvestigationResponseDto> geminiExplanationOpt) {

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
                retryReason, moneyAtRisk, geminiExplanationOpt
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
                .bankStatus(bank != null && bank.getBankStatus() != null ? bank.getBankStatus().name() : null)
                .bankUtr(bank != null ? bank.getUtrNumber() : null)
                .gatewayStatus(gateway != null && gateway.getGatewayStatus() != null ? gateway.getGatewayStatus().name() : null)
                .gatewayAuthStatus(gateway != null && gateway.getAuthStatus() != null ? gateway.getAuthStatus().name() : null)
                .gatewayCaptureStatus(gateway != null && gateway.getCaptureStatus() != null ? gateway.getCaptureStatus().name() : null)
                .merchantOrderStatus(merchantOrder != null && merchantOrder.getOrderStatus() != null ? merchantOrder.getOrderStatus().name() : null)
                .merchantFulfillmentStatus(merchantOrder != null && merchantOrder.getFulfillmentStatus() != null ? merchantOrder.getFulfillmentStatus().name() : null)
                .webhookDeliveryStatus(webhook != null && webhook.getDeliveryStatus() != null ? webhook.getDeliveryStatus().name() : null)
                .webhookHttpStatusCode(webhook != null ? webhook.getHttpStatusCode() : null)
                .settlementStatus(settlement != null && settlement.getSettlementStatus() != null ? settlement.getSettlementStatus().name() : null)
                .aiReport(aiReport)
                .geminiExplanation(geminiExplanationOpt.orElse(null))
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

        Optional<GeminiInvestigationResponseDto> geminiExplanationOpt = Optional.empty();
        if (assessment.isPresent() && assessment.get().getGeminiExplanation() != null && !assessment.get().getGeminiExplanation().isBlank()) {
            geminiExplanationOpt = Optional.of(GeminiInvestigationResponseDto.builder()
                    .summary(assessment.get().getGeminiExplanation())
                    .whatHappened(assessment.get().getGeminiExplanation())
                    .mlReasoning(assessment.get().getModelExplanation())
                    .build());
        }

        return aiInvestigationService.generateInvestigationReport(
                incident, payment, bank, gateway, merchantOrder, webhook, settlement, refund,
                evidenceDtos, timelineEvents, assessment.map(this::mapAssessmentToDto),
                Collections.emptyList(), isRetryProhibited, retryReason, moneyAtRisk,
                geminiExplanationOpt
        );
    }

    /**
     * Authoritative Java Safety & Resolution Decision Engine.
     *
     * ARCHITECTURAL RULE:
     * JAVA INVESTIGATES. ML CLASSIFIES. JAVA DECIDES. GEMINI EXPLAINS.
     *
     * The Random Forest ML model ONLY outputs incident classification and confidence.
     * ML does NOT make financial or operational recommendations.
     * ALL remediation actions, retry blocks, and refund decisions are computed
     * deterministically by Java safety rules and telemetry evidence.
     */
    public SuggestedAction determineJavaSafetyAction(
            String predictedRootCause,
            BigDecimal confidence,
            boolean isBankDebited,
            boolean isGatewayCaptured,
            boolean isGatewayFailed,
            boolean isMerchantPaid,
            boolean isMerchantCancelled,
            boolean isWebhookFailed,
            SettlementRecord settlement,
            RefundRecord refund) {

        // Rule 1: Financial Invariant - Active customer bank debit with cancelled order or failed gateway -> AUTO_REFUND_CUSTOMER
        if (isBankDebited && (isGatewayFailed || isMerchantCancelled)) {
            return SuggestedAction.AUTO_REFUND_CUSTOMER;
        }

        // Rule 2: Low ML Confidence (< 70%) -> Cannot automate risky actions; escalate to human review
        if (confidence != null && confidence.compareTo(ML_CONFIDENCE_THRESHOLD) < 0) {
            return SuggestedAction.MANUAL_BANK_ESCALATION;
        }

        // Rule 3: Missing Webhook (Gateway captured, webhook dropped/failed, merchant unpaid) -> RESEND_WEBHOOK
        if (isGatewayCaptured && isWebhookFailed && !isMerchantPaid) {
            return SuggestedAction.RESEND_WEBHOOK;
        }
        if ("MISSING_WEBHOOK".equalsIgnoreCase(predictedRootCause) && isGatewayCaptured && !isMerchantPaid) {
            return SuggestedAction.RESEND_WEBHOOK;
        }

        // Rule 4: Duplicate Payment -> AUTO_REFUND_CUSTOMER
        if ("DUPLICATE_PAYMENT".equalsIgnoreCase(predictedRootCause)) {
            return SuggestedAction.AUTO_REFUND_CUSTOMER;
        }

        // Rule 5: Order Payment Conflict (cart expired/cancelled, funds debited/captured) -> AUTO_REFUND_CUSTOMER
        if ("ORDER_PAYMENT_CONFLICT".equalsIgnoreCase(predictedRootCause) && (isBankDebited || isGatewayCaptured)) {
            return SuggestedAction.AUTO_REFUND_CUSTOMER;
        }

        // Rule 6: Settlement discrepancy -> FORCE_SETTLE_MERCHANT
        if ((settlement != null && settlement.getSettlementStatus() == SettlementStatus.DISCREPANCY)
                || "SETTLEMENT_MISMATCH".equalsIgnoreCase(predictedRootCause)) {
            return SuggestedAction.FORCE_SETTLE_MERCHANT;
        }

        // Rule 7: Synchronized normal or delayed payment where merchant received confirmation -> NO_ACTION_REQUIRED
        if (("NORMAL".equalsIgnoreCase(predictedRootCause) || "DELAYED_CONFIRMATION".equalsIgnoreCase(predictedRootCause)) && isMerchantPaid) {
            return SuggestedAction.NO_ACTION_REQUIRED;
        }

        // Rule 8: If bank debited in any unhandled failure -> protect customer funds
        if (isBankDebited && !isMerchantPaid) {
            return SuggestedAction.AUTO_REFUND_CUSTOMER;
        }

        return SuggestedAction.MANUAL_BANK_ESCALATION;
    }
}
