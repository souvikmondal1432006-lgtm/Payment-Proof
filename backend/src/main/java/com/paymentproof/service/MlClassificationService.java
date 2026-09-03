package com.paymentproof.service;

import com.paymentproof.dto.MlAssessmentDto;
import com.paymentproof.dto.MlFeatureRequestDto;
import com.paymentproof.entity.enums.SuggestedAction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class MlClassificationService {

    public MlAssessmentDto classify(MlFeatureRequestDto request) {
        String bankStatus = request.getBankStatus() != null ? request.getBankStatus().toUpperCase() : "UNKNOWN";
        String gatewayStatus = request.getGatewayStatus() != null ? request.getGatewayStatus().toUpperCase() : "UNKNOWN";
        String merchantStatus = request.getMerchantStatus() != null ? request.getMerchantStatus().toUpperCase() : "UNKNOWN";
        String webhookStatus = request.getWebhookStatus() != null ? request.getWebhookStatus().toUpperCase() : "UNKNOWN";
        int webhookAttempts = request.getWebhookAttempts() != null ? request.getWebhookAttempts() : 0;
        int bankLatency = request.getBankLatencyMs() != null ? request.getBankLatencyMs() : 0;

        String rootCause;
        BigDecimal anomalyScore;
        BigDecimal confidenceScore;
        SuggestedAction suggestedAction;
        String explanation;

        if (("SUCCESS".equals(bankStatus) || "DEBITED".equals(bankStatus)) && 
            ("FAILED".equals(gatewayStatus) || "TIMED_OUT".equals(gatewayStatus))) {
            rootCause = "GATEWAY_DOWNSTREAM_DISCONNECT_POST_DEBIT";
            anomalyScore = BigDecimal.valueOf(0.9650);
            confidenceScore = BigDecimal.valueOf(0.9820);
            suggestedAction = SuggestedAction.AUTO_REFUND_CUSTOMER;
            explanation = "Bank successfully debited customer funds, but gateway timed out or closed socket. Merchant cancelled order.";
        } 
        else if ("SUCCESS".equals(gatewayStatus) && ("FAILED".equals(webhookStatus) || "DROPPED".equals(webhookStatus) || "TIMED_OUT".equals(webhookStatus))) {
            rootCause = "MERCHANT_WEBHOOK_ENDPOINT_OUTAGE";
            anomalyScore = BigDecimal.valueOf(0.8840);
            confidenceScore = BigDecimal.valueOf(0.9610);
            suggestedAction = SuggestedAction.RESEND_WEBHOOK;
            explanation = "Gateway captured payment successfully, but merchant webhook delivery failed after " + webhookAttempts + " attempts.";
        } 
        else if ("CANCELLED".equals(merchantStatus) && "SUCCESS".equals(gatewayStatus) && bankLatency > 100000) {
            rootCause = "PREMATURE_MERCHANT_SESSION_EXPIRY";
            anomalyScore = BigDecimal.valueOf(0.8920);
            confidenceScore = BigDecimal.valueOf(0.9750);
            suggestedAction = SuggestedAction.AUTO_REFUND_CUSTOMER;
            explanation = "Merchant checkout window expired prior to asynchronous 3DS authorization completion.";
        } 
        else if ("SUCCESS".equals(bankStatus) && "PENDING".equals(gatewayStatus) && "CANCELLED".equals(merchantStatus)) {
            rootCause = "MULTI_SYSTEM_STATE_DESYNCHRONIZATION";
            anomalyScore = BigDecimal.valueOf(0.9850);
            confidenceScore = BigDecimal.valueOf(0.9920);
            suggestedAction = SuggestedAction.AUTO_REFUND_CUSTOMER;
            explanation = "Contradiction detected: Bank debited, Gateway pending, Merchant cancelled, Webhook dropped.";
        } 
        else if ("REVERSED".equals(bankStatus)) {
            rootCause = "ISSUER_SIDE_AUTO_REVERSAL_OR_CHARGEBACK";
            anomalyScore = BigDecimal.valueOf(0.9410);
            confidenceScore = BigDecimal.valueOf(0.5820);
            suggestedAction = SuggestedAction.MANUAL_BANK_ESCALATION;
            explanation = "Issuer initiated debit reversal or chargeback inquiry requiring clearing desk escalation.";
        } 
        else if (bankLatency > 60000) {
            rootCause = "PSP_DOWNSTREAM_LATENCY_SPIKE";
            anomalyScore = BigDecimal.valueOf(0.4200);
            confidenceScore = BigDecimal.valueOf(0.9400);
            suggestedAction = SuggestedAction.NO_ACTION_REQUIRED;
            explanation = "High latency during banking switch processing; payment eventually completed.";
        } 
        else if ("SUCCESS".equals(bankStatus) && "SUCCESS".equals(gatewayStatus) && "PAID".equals(merchantStatus)) {
            rootCause = "NORMAL_TRANSACTION_SYNCHRONIZED";
            anomalyScore = BigDecimal.valueOf(0.0500);
            confidenceScore = BigDecimal.valueOf(0.9980);
            suggestedAction = SuggestedAction.NO_ACTION_REQUIRED;
            explanation = "All systems report synchronized successful states.";
        } 
        else {
            rootCause = "UNCLASSIFIED_TELEMETRY_VARIANCE";
            anomalyScore = BigDecimal.valueOf(0.6500);
            confidenceScore = BigDecimal.valueOf(0.7500);
            suggestedAction = SuggestedAction.MANUAL_BANK_ESCALATION;
            explanation = "Complex telemetry patterns detected across providers.";
        }

        return MlAssessmentDto.builder()
                .assessmentId("mla_stub_" + System.currentTimeMillis())
                .incidentId(request.getIncidentId())
                .paymentId(request.getPaymentId())
                .modelVersion("reconcile-net-v2.1-stub")
                .predictedRootCause(rootCause)
                .anomalyScore(anomalyScore.setScale(4, RoundingMode.HALF_UP))
                .confidenceScore(confidenceScore.setScale(4, RoundingMode.HALF_UP))
                .suggestedAction(suggestedAction)
                .modelExplanation(explanation)
                .assessedAt(LocalDateTime.now())
                .build();
    }
}
