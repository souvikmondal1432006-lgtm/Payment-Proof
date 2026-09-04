package com.paymentproof.service;

import com.paymentproof.dto.*;
import com.paymentproof.entity.*;
import com.paymentproof.entity.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInvestigationService {

    /**
     * Synthesizes multi-party evidence, ML assessment, timeline events, and deterministic ground facts
     * into a structured, plain-English AI Investigation Report adhering to:
     * OBSERVE → COLLECT EVIDENCE → REASON → EXPLAIN → RECOMMEND
     */
    public AiInvestigationReportDto generateInvestigationReport(
            IncidentCase incident,
            Payment payment,
            BankRecord bank,
            GatewayRecord gateway,
            MerchantOrderRecord merchantOrder,
            WebhookRecord webhook,
            SettlementRecord settlement,
            RefundRecord refund,
            List<InvestigationEvidenceDto> evidenceDtos,
            List<TimelineEventDto> timelineEvents,
            Optional<MlAssessmentDto> mlAssessmentOpt,
            List<String> contradictions,
            boolean isRetryProhibited,
            String retryReason,
            BigDecimal moneyAtRisk) {
        return generateInvestigationReport(
                incident, payment, bank, gateway, merchantOrder, webhook, settlement, refund,
                evidenceDtos, timelineEvents, mlAssessmentOpt, contradictions, isRetryProhibited,
                retryReason, moneyAtRisk, Optional.empty()
        );
    }

    /**
     * Overloaded method incorporating optional Gemini Explanation Assistant synthesis.
     * Note: Gemini is strictly explanatory; Java remains the sole authoritative decision-maker.
     */
    public AiInvestigationReportDto generateInvestigationReport(
            IncidentCase incident,
            Payment payment,
            BankRecord bank,
            GatewayRecord gateway,
            MerchantOrderRecord merchantOrder,
            WebhookRecord webhook,
            SettlementRecord settlement,
            RefundRecord refund,
            List<InvestigationEvidenceDto> evidenceDtos,
            List<TimelineEventDto> timelineEvents,
            Optional<MlAssessmentDto> mlAssessmentOpt,
            List<String> contradictions,
            boolean isRetryProhibited,
            String retryReason,
            BigDecimal moneyAtRisk,
            Optional<GeminiInvestigationResponseDto> geminiExplanationOpt) {

        log.info("Generating authoritative AI Investigation Report for incident: {} (Gemini present: {})",
                incident.getIncidentId(), geminiExplanationOpt.isPresent());

        boolean isBankDebited = (bank != null && (bank.getBankStatus() == BankStatus.SUCCESS || bank.getBankStatus() == BankStatus.DEBITED));
        boolean isGatewayCaptured = (gateway != null && gateway.getCaptureStatus() == CaptureStatus.CAPTURED);
        boolean isGatewayFailed = (gateway != null && (gateway.getGatewayStatus() == GatewayStatus.FAILED || gateway.getGatewayStatus() == GatewayStatus.TIMED_OUT));
        boolean isMerchantPaid = (merchantOrder != null && merchantOrder.getOrderStatus() == OrderStatus.PAID);
        boolean isMerchantCancelled = (merchantOrder != null && merchantOrder.getOrderStatus() == OrderStatus.CANCELLED);
        boolean isWebhookFailed = (webhook != null && (webhook.getDeliveryStatus() == WebhookDeliveryStatus.FAILED || webhook.getDeliveryStatus() == WebhookDeliveryStatus.DROPPED));

        BigDecimal confidence = mlAssessmentOpt.map(MlAssessmentDto::getEffectiveConfidence).orElse(null);
        String predictedClass = mlAssessmentOpt.map(MlAssessmentDto::getEffectivePredictedClass).orElse("UNAVAILABLE");
        List<ContributingSignalDto> topSignals = mlAssessmentOpt.map(MlAssessmentDto::getTopContributingSignals).orElse(Collections.emptyList());
        Map<String, BigDecimal> classProbs = mlAssessmentOpt.map(MlAssessmentDto::getClassProbabilities).orElse(Collections.emptyMap());

        // 1. WHAT HAPPENED (Deterministic chronological plain-English narrative generated solely by Java)
        String whatHappened = buildWhatHappenedNarrative(payment, bank, gateway, merchantOrder, webhook, settlement, refund, predictedClass);

        // 2. WHY WE THINK THIS (Deterministic multi-system synthesis generated solely by Java)
        String whyWeThinkThis = buildWhyWeThinkThis(bank, gateway, merchantOrder, webhook, settlement, refund, contradictions, predictedClass, confidence);

        // 3. WHAT IS UNCERTAIN (Deterministic epistemic humility & unresolved questions generated solely by Java)
        String whatIsUncertain = buildWhatIsUncertain(bank, gateway, merchantOrder, webhook, settlement, refund, mlAssessmentOpt);

        // 4. RECOMMENDED ACTION (Safest next operational step - STRICTLY determined by Java rules)
        SuggestedAction recommendedAction = determineSafestAction(isBankDebited, isGatewayCaptured, isGatewayFailed, isMerchantPaid, isMerchantCancelled, isWebhookFailed, settlement, refund, predictedClass);

        // 5. AUDITABLE DECISION FACTORS (Concise, structured factors without hidden chain-of-thought)
        List<String> decisionFactors = buildAuditableDecisionFactors(bank, gateway, merchantOrder, webhook, settlement, refund, isRetryProhibited, mlAssessmentOpt);

        return AiInvestigationReportDto.builder()
                .incidentId(incident.getIncidentId())
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .merchantId(payment.getMerchantId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .incidentClassification(incident.getIncidentType())
                .severity(incident.getSeverity())
                .investigationStatus(incident.getCaseStatus())
                
                .whatHappened(whatHappened)
                .whyWeThinkThis(whyWeThinkThis)
                .evidence(evidenceDtos)
                .whatIsUncertain(whatIsUncertain)
                .recommendedAction(recommendedAction)
                .moneyAtRisk(moneyAtRisk)
                .confidence(confidence)
                
                .isRetryProhibited(isRetryProhibited)
                .retryProhibitionReason(retryReason)
                .decisionFactors(decisionFactors)
                .contradictionsDetected(contradictions)
                .topContributingSignals(topSignals)
                .classProbabilities(classProbs)
                .timelineEventsCount(timelineEvents != null ? timelineEvents.size() : 0)
                .geminiExplanation(geminiExplanationOpt.orElse(null))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private String buildWhatHappenedNarrative(
            Payment payment,
            BankRecord bank,
            GatewayRecord gateway,
            MerchantOrderRecord merchantOrder,
            WebhookRecord webhook,
            SettlementRecord settlement,
            RefundRecord refund,
            String predictedClass) {

        String amountFormatted = String.format("INR %s", payment.getAmount());

        if ("BANK_DEBIT_GATEWAY_FAILURE".equalsIgnoreCase(predictedClass) ||
                (bank != null && bank.getBankStatus() == BankStatus.SUCCESS && gateway != null && gateway.getGatewayStatus() == GatewayStatus.FAILED)) {
            return String.format("The customer appears to have been debited %s at %s via %s (UTR: %s), but the payment gateway experienced an upstream timeout or connection drop. Consequently, the merchant order management system never received confirmation and cancelled the cart.",
                    amountFormatted,
                    bank != null ? bank.getBankName() : "the issuing bank",
                    payment.getPaymentMethod(),
                    bank != null && bank.getUtrNumber() != null ? bank.getUtrNumber() : "UNCONFIRMED");
        }

        if ("MISSING_WEBHOOK".equalsIgnoreCase(predictedClass) ||
                (gateway != null && gateway.getCaptureStatus() == CaptureStatus.CAPTURED && webhook != null && webhook.getDeliveryStatus() == WebhookDeliveryStatus.DROPPED)) {
            return String.format("The payment of %s was successfully authorized and captured by the gateway (%s), but the asynchronous webhook notification dropped after %d attempt(s) with HTTP %s. The merchant order remains unfulfilled in PENDING_PAYMENT status.",
                    amountFormatted,
                    gateway != null ? gateway.getGatewayName() : "payment gateway",
                    webhook != null ? webhook.getAttemptCount() : 1,
                    webhook != null && webhook.getHttpStatusCode() != null ? webhook.getHttpStatusCode() : 504);
        }

        if ("DELAYED_CONFIRMATION".equalsIgnoreCase(predictedClass) ||
                (bank != null && bank.getNetworkLatencyMs() != null && bank.getNetworkLatencyMs() > 30000)) {
            double latencySec = (bank != null && bank.getNetworkLatencyMs() != null) ? bank.getNetworkLatencyMs() / 1000.0 : 45.0;
            return String.format("The core banking switch experienced extreme latency of %.1fs during 3DS processing. Although the transaction was eventually captured, the confirmation arrived after the checkout session timed out on the client.",
                    latencySec);
        }

        if ("DUPLICATE_PAYMENT".equalsIgnoreCase(predictedClass)) {
            return String.format("Evidence indicates the customer encountered a slow screen and retried checkout, resulting in multiple distinct successful bank debits for the single merchant order reference %s.",
                    payment.getOrderId());
        }

        if ("REFUND_UNCERTAINTY".equalsIgnoreCase(predictedClass) ||
                (refund != null && refund.getRefundStatus() == RefundStatus.MANUAL_INTERVENTION_REQUIRED)) {
            return String.format("A refund of %s was initiated at the gateway under ARN %s, but the issuer banking reversal network has not acknowledged customer credit within the standard clearing window.",
                    amountFormatted,
                    refund != null && refund.getRefundArn() != null ? refund.getRefundArn() : "PENDING");
        }

        if ("SETTLEMENT_MISMATCH".equalsIgnoreCase(predictedClass) ||
                (settlement != null && settlement.getSettlementStatus() == SettlementStatus.DISCREPANCY)) {
            return String.format("Payment of %s was captured cleanly from the customer, but the merchant settlement ledger batch calculation exhibits an unexplained variance in MDR fee or net payout amount.",
                    amountFormatted);
        }

        if ("ORDER_PAYMENT_CONFLICT".equalsIgnoreCase(predictedClass)) {
            return String.format("3DS authorization required longer than the merchant's 5-minute inventory reservation window. The merchant cart expired and released reserved goods moments before the payment capture callback succeeded.",
                    amountFormatted);
        }

        if ("NORMAL".equalsIgnoreCase(predictedClass) ||
                (bank != null && bank.getBankStatus() == BankStatus.SUCCESS && gateway != null && gateway.getGatewayStatus() == GatewayStatus.SUCCESS && merchantOrder != null && merchantOrder.getOrderStatus() == OrderStatus.PAID)) {
            return String.format("All provider systems (Bank, Gateway, Merchant OMS, Webhook) are fully synchronized in SUCCESS state for %s. The incident alert was most likely a transient false alarm.",
                    amountFormatted);
        }

        return String.format("Conflicting telemetry across distributed providers was observed for %s. Telemetry sources report incompatible lifecycle states requiring operator intervention.",
                amountFormatted);
    }

    private String buildWhyWeThinkThis(
            BankRecord bank,
            GatewayRecord gateway,
            MerchantOrderRecord merchantOrder,
            WebhookRecord webhook,
            SettlementRecord settlement,
            RefundRecord refund,
            List<String> contradictions,
            String predictedClass,
            BigDecimal confidence) {

        StringBuilder sb = new StringBuilder();
        sb.append("Evidence synthesis across multi-party telemetries indicates: ");

        if (bank != null) {
            sb.append(String.format("Bank switch reported status %s (UTR: %s, Latency: %dms). ",
                    bank.getBankStatus(),
                    bank.getUtrNumber() != null ? bank.getUtrNumber() : "NONE",
                    bank.getNetworkLatencyMs() != null ? bank.getNetworkLatencyMs() : 0));
        }

        if (gateway != null) {
            sb.append(String.format("Gateway reported status %s with capture status %s (Latency: %dms). ",
                    gateway.getGatewayStatus(), gateway.getCaptureStatus(),
                    gateway.getProcessingLatencyMs() != null ? gateway.getProcessingLatencyMs() : 0));
        }

        if (merchantOrder != null) {
            sb.append(String.format("Merchant OMS recorded order status %s with fulfillment %s (Reason: %s). ",
                    merchantOrder.getOrderStatus(), merchantOrder.getFulfillmentStatus(),
                    merchantOrder.getCancellationReason() != null ? merchantOrder.getCancellationReason() : "NONE"));
        }

        if (webhook != null) {
            sb.append(String.format("Webhook delivery resulted in %s (HTTP %s, %d attempts). ",
                    webhook.getDeliveryStatus(),
                    webhook.getHttpStatusCode() != null ? webhook.getHttpStatusCode() : "NONE",
                    webhook.getAttemptCount() != null ? webhook.getAttemptCount() : 0));
        }

        if (settlement != null) {
            sb.append(String.format("Settlement batch status is %s. ", settlement.getSettlementStatus()));
        }

        if (confidence != null) {
            sb.append(String.format("Statistical ML classifier assigned %s%% confidence to '%s'.",
                    confidence.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
                    predictedClass));
        }

        return sb.toString();
    }

    private String buildWhatIsUncertain(
            BankRecord bank,
            GatewayRecord gateway,
            MerchantOrderRecord merchantOrder,
            WebhookRecord webhook,
            SettlementRecord settlement,
            RefundRecord refund,
            Optional<MlAssessmentDto> mlAssessmentOpt) {

        List<String> uncertainties = new ArrayList<>();

        if (bank != null && bank.getBankStatus() == BankStatus.SUCCESS && merchantOrder != null && merchantOrder.getOrderStatus() == OrderStatus.CANCELLED) {
            uncertainties.add("Unable to confirm whether the merchant inventory was permanently reallocated to another customer or can be re-reserved.");
            uncertainties.add("Unable to confirm whether the customer's bank has scheduled an automatic T+5 clearing reversal.");
        }

        if (webhook != null && webhook.getDeliveryStatus() == WebhookDeliveryStatus.DROPPED) {
            uncertainties.add("Unable to confirm if the merchant backend processed the transaction out-of-band via manual polling.");
        }

        if (refund != null && refund.getRefundStatus() == RefundStatus.MANUAL_INTERVENTION_REQUIRED) {
            uncertainties.add("Unable to confirm if the issuing bank network accepted the reversal payload or rejected it due to an account freeze.");
        }

        if (mlAssessmentOpt.isEmpty()) {
            uncertainties.add("AI prediction unavailable — multi-party evidence is still available. Automated ML model evaluation was offline during this analysis. No automatic payment action was taken.");
        } else if (mlAssessmentOpt.get().getEffectiveConfidence() != null && mlAssessmentOpt.get().getEffectiveConfidence().compareTo(BigDecimal.valueOf(0.70)) < 0) {
            uncertainties.add("ML classification confidence is below 70%, indicating overlapping failure signatures.");
        }

        if (uncertainties.isEmpty()) {
            return "No critical telemetry uncertainties identified; multi-system evidence provides high clarity.";
        }

        return String.join(" ", uncertainties);
    }

    private SuggestedAction determineSafestAction(
            boolean isBankDebited,
            boolean isGatewayCaptured,
            boolean isGatewayFailed,
            boolean isMerchantPaid,
            boolean isMerchantCancelled,
            boolean isWebhookFailed,
            SettlementRecord settlement,
            RefundRecord refund,
            String predictedClass) {

        if (isBankDebited && (isGatewayFailed || isMerchantCancelled)) {
            return SuggestedAction.AUTO_REFUND_CUSTOMER;
        }

        if (isGatewayCaptured && isWebhookFailed) {
            return SuggestedAction.RESEND_WEBHOOK;
        }

        if ("DUPLICATE_PAYMENT".equalsIgnoreCase(predictedClass)) {
            return SuggestedAction.AUTO_REFUND_CUSTOMER;
        }

        if (settlement != null && settlement.getSettlementStatus() == SettlementStatus.DISCREPANCY) {
            return SuggestedAction.FORCE_SETTLE_MERCHANT;
        }

        if ("NORMAL".equalsIgnoreCase(predictedClass) && isMerchantPaid) {
            return SuggestedAction.NO_ACTION_REQUIRED;
        }

        return SuggestedAction.MANUAL_BANK_ESCALATION;
    }

    private List<String> buildAuditableDecisionFactors(
            BankRecord bank,
            GatewayRecord gateway,
            MerchantOrderRecord merchantOrder,
            WebhookRecord webhook,
            SettlementRecord settlement,
            RefundRecord refund,
            boolean isRetryProhibited,
            Optional<MlAssessmentDto> mlAssessmentOpt) {

        List<String> factors = new ArrayList<>();

        if (bank != null) {
            factors.add(String.format("Bank Telemetry: status=%s, utr=%s, debited_amount=%s",
                    bank.getBankStatus(), bank.getUtrNumber(), bank.getDebitedAmount()));
        }

        if (gateway != null) {
            factors.add(String.format("Gateway Telemetry: status=%s, capture=%s, latency=%sms",
                    gateway.getGatewayStatus(), gateway.getCaptureStatus(), gateway.getProcessingLatencyMs()));
        }

        if (merchantOrder != null) {
            factors.add(String.format("Merchant OMS: order_status=%s, fulfillment=%s, cancellation_reason=%s",
                    merchantOrder.getOrderStatus(), merchantOrder.getFulfillmentStatus(), merchantOrder.getCancellationReason()));
        }

        if (webhook != null) {
            factors.add(String.format("Webhook Engine: delivery_status=%s, http_code=%s, attempts=%s",
                    webhook.getDeliveryStatus(), webhook.getHttpStatusCode(), webhook.getAttemptCount()));
        }

        factors.add(String.format("Safety Invariant: is_retry_prohibited=%b", isRetryProhibited));

        if (mlAssessmentOpt.isPresent()) {
            MlAssessmentDto ml = mlAssessmentOpt.get();
            factors.add(String.format("ML Advisory: model=%s, root_cause=%s, confidence=%s",
                    ml.getModelVersion(), ml.getEffectivePredictedClass(), ml.getEffectiveConfidence()));
        } else {
            factors.add("ML Advisory: service_offline=true");
        }

        return factors;
    }
}
