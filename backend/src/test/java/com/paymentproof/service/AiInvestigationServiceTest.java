package com.paymentproof.service;

import com.paymentproof.dto.AiInvestigationReportDto;
import com.paymentproof.dto.InvestigationEvidenceDto;
import com.paymentproof.dto.MlAssessmentDto;
import com.paymentproof.entity.*;
import com.paymentproof.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AiInvestigationServiceTest {

    private AiInvestigationService aiInvestigationService;
    private IncidentCase mockIncident;
    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        aiInvestigationService = new AiInvestigationService();

        mockPayment = Payment.builder()
                .paymentId("pay_ai_001")
                .merchantId("merch_flipkart")
                .customerId("cust_rahul")
                .orderId("ORD-9901")
                .amount(BigDecimal.valueOf(4500.00))
                .currency("INR")
                .paymentMethod("UPI")
                .status(PaymentStatus.FLAGGED)
                .initiatedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        mockIncident = IncidentCase.builder()
                .incidentId("inc_ai_001")
                .paymentId("pay_ai_001")
                .incidentType(IncidentType.BANK_DEBIT_GATEWAY_FAILURE)
                .severity(Severity.CRITICAL)
                .caseStatus(CaseStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("AI INVESTIGATOR: Structured Report for Ghost Debit (Bank debited, Gateway timeout, Cart cancelled)")
    void testGenerateReport_GhostDebit() {
        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_01")
                .paymentId("pay_ai_001")
                .bankName("HDFC_BANK")
                .bankStatus(BankStatus.SUCCESS)
                .utrNumber("UTR414960264709")
                .debitedAmount(BigDecimal.valueOf(4500.00))
                .networkLatencyMs(420)
                .bankTimestamp(LocalDateTime.now().minusMinutes(9))
                .build();

        GatewayRecord gateway = GatewayRecord.builder()
                .gatewayRecordId("gw_01")
                .paymentId("pay_ai_001")
                .gatewayName("RAZORPAY")
                .gatewayStatus(GatewayStatus.FAILED)
                .captureStatus(CaptureStatus.FAILED)
                .errorCode("GATEWAY_TIMEOUT")
                .processingLatencyMs(65000)
                .build();

        MerchantOrderRecord merchantOrder = MerchantOrderRecord.builder()
                .merchantOrderRecordId("mor_01")
                .paymentId("pay_ai_001")
                .merchantId("merch_flipkart")
                .merchantOrderId("ORD-9901")
                .orderStatus(OrderStatus.CANCELLED)
                .fulfillmentStatus(FulfillmentStatus.CANCELLED)
                .cancellationReason("SESSION_TIMEOUT_NO_PROOF")
                .build();

        MlAssessmentDto ml = MlAssessmentDto.builder()
                .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                .confidenceScore(BigDecimal.valueOf(0.9850))
                .build();

        AiInvestigationReportDto report = aiInvestigationService.generateInvestigationReport(
                mockIncident,
                mockPayment,
                bank,
                gateway,
                merchantOrder,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.of(ml),
                List.of("Ghost Debit detected"),
                true,
                "Strict safety invariant",
                BigDecimal.valueOf(4500.00)
        );

        assertNotNull(report);
        // Verify all 6 core sections exist and are non-empty
        assertNotNull(report.getWhatHappened());
        assertTrue(report.getWhatHappened().contains("The customer appears to have been debited INR 4500"));
        assertTrue(report.getWhatHappened().contains("UTR414960264709"));

        assertNotNull(report.getWhyWeThinkThis());
        assertTrue(report.getWhyWeThinkThis().contains("Bank switch reported status SUCCESS"));
        assertTrue(report.getWhyWeThinkThis().contains("Gateway reported status FAILED"));

        assertNotNull(report.getWhatIsUncertain());
        assertTrue(report.getWhatIsUncertain().contains("Unable to confirm"));

        assertEquals(SuggestedAction.AUTO_REFUND_CUSTOMER, report.getRecommendedAction());
        assertEquals(BigDecimal.valueOf(4500.00), report.getMoneyAtRisk());
        assertEquals(BigDecimal.valueOf(0.9850), report.getConfidence());
        assertTrue(report.isRetryProhibited());

        // Auditable decision factors
        assertNotNull(report.getDecisionFactors());
        assertTrue(report.getDecisionFactors().size() >= 3);
        assertTrue(report.getDecisionFactors().stream().anyMatch(f -> f.contains("UTR414960264709")));
    }

    @Test
    @DisplayName("AI INVESTIGATOR: Structured Report for Missing Webhook")
    void testGenerateReport_MissingWebhook() {
        GatewayRecord gateway = GatewayRecord.builder()
                .gatewayRecordId("gw_02")
                .paymentId("pay_ai_001")
                .gatewayName("CASHFREE")
                .gatewayStatus(GatewayStatus.SUCCESS)
                .captureStatus(CaptureStatus.CAPTURED)
                .build();

        WebhookRecord webhook = WebhookRecord.builder()
                .webhookId("wh_02")
                .paymentId("pay_ai_001")
                .deliveryStatus(WebhookDeliveryStatus.DROPPED)
                .httpStatusCode(504)
                .attemptCount(3)
                .build();

        MerchantOrderRecord merchantOrder = MerchantOrderRecord.builder()
                .merchantOrderRecordId("mor_02")
                .paymentId("pay_ai_001")
                .orderStatus(OrderStatus.PENDING_PAYMENT)
                .fulfillmentStatus(FulfillmentStatus.UNFULFILLED)
                .build();

        MlAssessmentDto ml = MlAssessmentDto.builder()
                .predictedRootCause("MISSING_WEBHOOK")
                .confidenceScore(BigDecimal.valueOf(0.9620))
                .build();

        AiInvestigationReportDto report = aiInvestigationService.generateInvestigationReport(
                mockIncident,
                mockPayment,
                null,
                gateway,
                merchantOrder,
                webhook,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.of(ml),
                List.of("Dropped Webhook"),
                false,
                "Safe",
                BigDecimal.ZERO
        );

        assertNotNull(report);
        assertTrue(report.getWhatHappened().contains("successfully authorized and captured"));
        assertTrue(report.getWhatHappened().contains("webhook notification dropped after 3 attempt(s) with HTTP 504"));
        assertEquals(SuggestedAction.RESEND_WEBHOOK, report.getRecommendedAction());
    }

    @Test
    @DisplayName("AI INVESTIGATOR: Epistemic Humility when ML is Offline")
    void testGenerateReport_MlOffline() {
        AiInvestigationReportDto report = aiInvestigationService.generateInvestigationReport(
                mockIncident,
                mockPayment,
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.empty(),
                List.of("No telemetry divergence"),
                false,
                "Safe",
                BigDecimal.ZERO
        );

        assertNotNull(report);
        assertNull(report.getConfidence(), "Confidence must be null when ML service is offline");
        assertTrue(report.getWhatIsUncertain().contains("Automated ML model evaluation was offline"));
        assertTrue(report.getDecisionFactors().stream().anyMatch(f -> f.contains("service_offline=true")));
    }
}
