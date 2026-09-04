package com.paymentproof.service;

import com.paymentproof.client.MlServiceClient;
import com.paymentproof.dto.ContributingSignalDto;
import com.paymentproof.dto.InvestigationResultDto;
import com.paymentproof.dto.MlAssessmentDto;
import com.paymentproof.entity.*;
import com.paymentproof.entity.enums.*;
import com.paymentproof.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import com.paymentproof.dto.GeminiPromptPayloadDto;
import com.paymentproof.dto.GeminiInvestigationResponseDto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvestigationServiceTest {

    @Mock
    private IncidentCaseRepository incidentCaseRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BankRecordRepository bankRecordRepository;
    @Mock
    private GatewayRecordRepository gatewayRecordRepository;
    @Mock
    private MerchantOrderRecordRepository merchantOrderRecordRepository;
    @Mock
    private WebhookRecordRepository webhookRecordRepository;
    @Mock
    private SettlementRecordRepository settlementRecordRepository;
    @Mock
    private RefundRecordRepository refundRecordRepository;
    @Mock
    private InvestigationEvidenceRepository evidenceRepository;
    @Mock
    private MlAssessmentRepository mlAssessmentRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private MlServiceClient mlServiceClient;
    @Mock
    private TimelineService timelineService;
    @Mock
    private GeminiInvestigationService geminiInvestigationService;
    @org.mockito.Spy
    private AiInvestigationService aiInvestigationService = new AiInvestigationService();

    @InjectMocks
    private InvestigationService investigationService;

    private Payment mockPayment;
    private IncidentCase mockIncident;

    @BeforeEach
    void setUp() {
        mockPayment = Payment.builder()
                .paymentId("pay_test_001")
                .merchantId("merch_flipkart")
                .customerId("cust_rahul")
                .orderId("ORD-9901")
                .amount(BigDecimal.valueOf(4500.00))
                .currency("INR")
                .paymentMethod("UPI")
                .status(PaymentStatus.FLAGGED)
                .initiatedAt(LocalDateTime.now().minusMinutes(10))
                .updatedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        mockIncident = IncidentCase.builder()
                .incidentId("inc_test_001")
                .paymentId("pay_test_001")
                .incidentType(IncidentType.BANK_DEBIT_GATEWAY_FAILURE)
                .severity(Severity.CRITICAL)
                .caseStatus(CaseStatus.OPEN)
                .triggerSource(TriggerSource.CUSTOMER_TICKET)
                .title("Ghost Debit on Order ORD-9901")
                .description("Customer debited INR 4500 but gateway reported timeout.")
                .openedAt(LocalDateTime.now().minusMinutes(8))
                .updatedAt(LocalDateTime.now().minusMinutes(8))
                .build();

        org.mockito.Mockito.lenient().when(geminiInvestigationService.explainInvestigation(any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("PHASE 4: ML Success with High Confidence (>= 70%)")
    void testInvestigateIncident_MlSuccess() {
        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_001")
                .paymentId("pay_test_001")
                .bankName("HDFC_BANK")
                .bankStatus(BankStatus.DEBITED)
                .utrNumber("UTR449911223344")
                .debitedAmount(BigDecimal.valueOf(4500.00))
                .bankTimestamp(LocalDateTime.now().minusMinutes(9))
                .createdAt(LocalDateTime.now().minusMinutes(9))
                .build();

        GatewayRecord gateway = GatewayRecord.builder()
                .gatewayRecordId("gw_001")
                .paymentId("pay_test_001")
                .gatewayName("RAZORPAY")
                .gatewayStatus(GatewayStatus.FAILED)
                .errorCode("GATEWAY_TIMEOUT")
                .gatewayTimestamp(LocalDateTime.now().minusMinutes(8))
                .createdAt(LocalDateTime.now().minusMinutes(8))
                .build();

        MerchantOrderRecord merchantOrder = MerchantOrderRecord.builder()
                .merchantOrderRecordId("mor_001")
                .paymentId("pay_test_001")
                .merchantId("merch_flipkart")
                .merchantOrderId("ORD-9901")
                .orderStatus(OrderStatus.CANCELLED)
                .fulfillmentStatus(FulfillmentStatus.CANCELLED)
                .expectedAmount(BigDecimal.valueOf(4500.00))
                .cancellationReason("PAYMENT_TIMEOUT")
                .merchantUpdatedAt(LocalDateTime.now().minusMinutes(7))
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(bank));
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(gateway));
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(merchantOrder));
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());
        when(mlAssessmentRepository.findByIncidentId("inc_test_001")).thenReturn(Optional.empty());

        MlAssessmentDto mockMlResponse = MlAssessmentDto.builder()
                .assessmentId("mla_001")
                .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                .anomalyScore(BigDecimal.valueOf(0.9650))
                .confidenceScore(BigDecimal.valueOf(0.9820))
                .modelVersion("incident-classifier-v1.0.0-rf")
                .topContributingSignals(List.of(
                        ContributingSignalDto.builder().signalName("bank_status_debited").signalValue("DEBITED").importanceWeight(0.45).build()
                ))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(mockMlResponse));

        InvestigationResultDto result = investigationService.investigateIncident("inc_test_001");

        assertNotNull(result);
        assertEquals(CaseStatus.AI_ANALYZED, result.getInvestigationStatus());
        assertEquals("BANK_DEBIT_GATEWAY_FAILURE", result.getPredictedRootCause());
        assertEquals(BigDecimal.valueOf(0.9820), result.getConfidence());
        assertTrue(result.isRetryProhibited(), "Payment retry must be strictly prohibited when bank is debited");
        assertEquals(BigDecimal.valueOf(4500.00), result.getMoneyAtRisk());
        assertEquals(SuggestedAction.AUTO_REFUND_CUSTOMER, result.getRecommendedAction());

        verify(mlAssessmentRepository, times(1)).save(any(MlAssessment.class));
        verify(incidentCaseRepository, times(1)).save(mockIncident);
        verify(auditService, times(1)).logEvent(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("PHASE 4: ML Unavailable / Connection Failure — DO NOT fail app, set NEEDS_REVIEW, zero fake ML result")
    void testInvestigateIncident_MlUnavailable() {
        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());

        // ML Service is DOWN / Unavailable
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.empty());

        InvestigationResultDto result = investigationService.investigateIncident("inc_test_001");

        assertNotNull(result);
        assertEquals(CaseStatus.NEEDS_REVIEW, result.getInvestigationStatus());
        assertEquals("UNAVAILABLE", result.getPredictedRootCause());
        assertNull(result.getConfidence(), "Confidence must be null when ML service is offline (no fake result)");
        assertNull(result.getAnomalyScore(), "Anomaly score must be null when ML service is offline");
        assertEquals("Automated classification unavailable. Evidence remains available for manual investigation.", result.getSummary());

        // Verify that NO fake ML assessment entity was persisted
        verify(mlAssessmentRepository, never()).save(any(MlAssessment.class));
        verify(incidentCaseRepository, times(1)).save(mockIncident);
    }

    @Test
    @DisplayName("PHASE 4: ML Timeout — Handled gracefully without inventing ML result")
    void testInvestigateIncident_MlTimeout() {
        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());

        // ML Service timed out
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.empty());

        InvestigationResultDto result = investigationService.investigateIncident("inc_test_001");

        assertNotNull(result);
        assertEquals(CaseStatus.NEEDS_REVIEW, result.getInvestigationStatus());
        assertEquals("Automated classification unavailable. Evidence remains available for manual investigation.", result.getSummary());
        verify(mlAssessmentRepository, never()).save(any(MlAssessment.class));
    }

    @Test
    @DisplayName("PHASE 4: ML Low Confidence (< 70%) — Escalate to NEEDS_REVIEW for operator review")
    void testInvestigateIncident_LowConfidencePrediction() {
        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());
        when(mlAssessmentRepository.findByIncidentId("inc_test_001")).thenReturn(Optional.empty());

        // Low confidence prediction (0.48 < 0.70)
        MlAssessmentDto lowConfResponse = MlAssessmentDto.builder()
                .assessmentId("mla_low_01")
                .predictedRootCause("UNRESOLVED")
                .confidenceScore(BigDecimal.valueOf(0.4800))
                .anomalyScore(BigDecimal.valueOf(0.7200))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(lowConfResponse));

        InvestigationResultDto result = investigationService.investigateIncident("inc_test_001");

        assertNotNull(result);
        assertEquals(CaseStatus.NEEDS_REVIEW, result.getInvestigationStatus(), "Low confidence prediction must trigger NEEDS_REVIEW status");
        assertEquals(BigDecimal.valueOf(0.4800), result.getConfidence());
        assertTrue(result.getSummary().contains("below threshold (70.0%)"));

        // Persists the actual low confidence assessment
        verify(mlAssessmentRepository, times(1)).save(any(MlAssessment.class));
    }

    @Test
    @DisplayName("PHASE 4: Java Authoritative Override of ML Output when Ground Facts Contradict ML")
    void testInvestigateIncident_DeterministicOverrideOfMl() {
        // Deterministic facts: Bank debited, Merchant cancelled cart
        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_005")
                .paymentId("pay_test_001")
                .bankStatus(BankStatus.SUCCESS)
                .utrNumber("UTR99887766")
                .debitedAmount(BigDecimal.valueOf(4500.00))
                .bankTimestamp(LocalDateTime.now().minusMinutes(5))
                .build();

        MerchantOrderRecord merchantOrder = MerchantOrderRecord.builder()
                .merchantOrderRecordId("mor_005")
                .paymentId("pay_test_001")
                .orderStatus(OrderStatus.CANCELLED)
                .cancellationReason("TIMEOUT_BEFORE_CALLBACK")
                .build();

        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(bank));
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(merchantOrder));
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());
        when(mlAssessmentRepository.findByIncidentId("inc_test_001")).thenReturn(Optional.empty());

        // ML model mistakenly advises DELAYED_CONFIRMATION
        MlAssessmentDto mistakenMl = MlAssessmentDto.builder()
                .assessmentId("mla_mistaken")
                .predictedRootCause("DELAYED_CONFIRMATION")
                .confidenceScore(BigDecimal.valueOf(0.9300))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(mistakenMl));

        InvestigationResultDto result = investigationService.investigateIncident("inc_test_001");

        assertNotNull(result);
        // Java Authoritatively overrides the recommendation to AUTO_REFUND_CUSTOMER
        assertEquals(SuggestedAction.AUTO_REFUND_CUSTOMER, result.getRecommendedAction(),
                "Java must override ML advice to AUTO_REFUND_CUSTOMER when bank is debited but merchant cancelled");
        assertTrue(result.isRetryProhibited(), "Retry must be prohibited when customer funds are debited");
        assertTrue(result.getSummary().contains("Overridden to auto-refund customer"));
    }

    @Test
    @DisplayName("TEST 1: Java investigation works when Gemini is unavailable")
    void testJavaInvestigationWorksWhenGeminiIsUnavailable() {
        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_001")
                .paymentId("pay_test_001")
                .bankStatus(BankStatus.SUCCESS)
                .debitedAmount(BigDecimal.valueOf(4500.00))
                .utrNumber("UTR414960264709")
                .build();

        MerchantOrderRecord merchantOrder = MerchantOrderRecord.builder()
                .merchantOrderRecordId("mor_001")
                .paymentId("pay_test_001")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(bank));
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(merchantOrder));
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());
        when(mlAssessmentRepository.findByIncidentId("inc_test_001")).thenReturn(Optional.empty());

        // Gemini is completely unavailable / returns empty
        when(geminiInvestigationService.explainInvestigation(any())).thenReturn(Optional.empty());

        InvestigationResultDto result = investigationService.investigateIncident("inc_test_001");

        assertNotNull(result);
        assertNull(result.getGeminiExplanation(), "Gemini explanation should be null when Gemini is unavailable");
        assertNotNull(result.getAiReport(), "Deterministic Java report must still be generated");
        assertEquals(SuggestedAction.AUTO_REFUND_CUSTOMER, result.getRecommendedAction(), "Java safety rule must still execute");
        assertTrue(result.isRetryProhibited(), "Java safety invariant must still be enforced");
    }

    @Test
    @DisplayName("TEST 2: Random Forest prediction is generated independently of Gemini")
    void testRandomForestPredictionGeneratedIndependentlyOfGemini() {
        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_001")
                .paymentId("pay_test_001")
                .bankStatus(BankStatus.SUCCESS)
                .debitedAmount(BigDecimal.valueOf(4500.00))
                .build();

        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(bank));
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());
        when(mlAssessmentRepository.findByIncidentId("inc_test_001")).thenReturn(Optional.empty());

        MlAssessmentDto rfPrediction = MlAssessmentDto.builder()
                .assessmentId("mla_rf_01")
                .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                .confidenceScore(BigDecimal.valueOf(0.9850))
                .topContributingSignals(List.of(ContributingSignalDto.builder().signalName("bank_status_SUCCESS").importanceWeight(0.4).build()))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(rfPrediction));

        // Call investigation
        InvestigationResultDto result = investigationService.investigateIncident("inc_test_001");

        // Verify Random Forest classified without Gemini interference
        verify(mlServiceClient, times(1)).classifyTelemetry(any());
        assertEquals("BANK_DEBIT_GATEWAY_FAILURE", result.getPredictedRootCause());
        assertEquals(BigDecimal.valueOf(0.9850), result.getConfidence());
    }

    @Test
    @DisplayName("TEST 3: Gemini receives the complete Java investigation result")
    void testGeminiReceivesCompleteJavaInvestigationResult() {
        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_001")
                .paymentId("pay_test_001")
                .bankStatus(BankStatus.SUCCESS)
                .debitedAmount(BigDecimal.valueOf(4500.00))
                .utrNumber("UTR414960264709")
                .networkLatencyMs(350)
                .build();

        MerchantOrderRecord merchantOrder = MerchantOrderRecord.builder()
                .merchantOrderRecordId("mor_001")
                .paymentId("pay_test_001")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(bank));
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(merchantOrder));
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());
        when(mlAssessmentRepository.findByIncidentId("inc_test_001")).thenReturn(Optional.empty());

        MlAssessmentDto rf = MlAssessmentDto.builder()
                .assessmentId("mla_rf_01")
                .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                .confidenceScore(BigDecimal.valueOf(0.99))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(rf));

        ArgumentCaptor<GeminiPromptPayloadDto> captor = ArgumentCaptor.forClass(GeminiPromptPayloadDto.class);
        when(geminiInvestigationService.explainInvestigation(captor.capture())).thenReturn(Optional.empty());

        investigationService.investigateIncident("inc_test_001");

        GeminiPromptPayloadDto payload = captor.getValue();
        assertNotNull(payload);
        // Verify payment facts
        assertEquals("pay_test_001", payload.getPaymentId());
        assertEquals(BigDecimal.valueOf(4500.00), payload.getAmount());
        // Verify evidence
        assertEquals("SUCCESS", payload.getBankStatus());
        assertEquals("CANCELLED", payload.getMerchantOrderStatus());
        // Verify ML results
        assertEquals("BANK_DEBIT_GATEWAY_FAILURE", payload.getMlPredictedClass());
        assertEquals(BigDecimal.valueOf(0.99), payload.getMlConfidence());
        // Verify Java safety decision
        assertTrue(payload.isRetryProhibited(), "Gemini must receive Java's authoritative retry prohibition");
        assertEquals("AUTO_REFUND_CUSTOMER", payload.getRecommendedResolution(), "Gemini must receive Java's authoritative recommendation");
    }

    @Test
    @DisplayName("TEST 4: Gemini cannot override Java safety decision")
    void testGeminiCannotOverrideJavaSafetyDecision() {
        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_001")
                .paymentId("pay_test_001")
                .bankStatus(BankStatus.SUCCESS)
                .debitedAmount(BigDecimal.valueOf(4500.00))
                .build();

        MerchantOrderRecord merchantOrder = MerchantOrderRecord.builder()
                .merchantOrderRecordId("mor_001")
                .paymentId("pay_test_001")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(bank));
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(merchantOrder));
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());
        when(mlAssessmentRepository.findByIncidentId("inc_test_001")).thenReturn(Optional.empty());

        // Gemini hypothetically returns a conflicting recommendation
        GeminiInvestigationResponseDto rogueGemini = GeminiInvestigationResponseDto.builder()
                .summary("Payment failed.")
                .whatHappened("Timeout occurred.")
                .recommendedOperatorAction("Retry the payment immediately.") // Rogue advice
                .build();
        when(geminiInvestigationService.explainInvestigation(any())).thenReturn(Optional.of(rogueGemini));

        InvestigationResultDto result = investigationService.investigateIncident("inc_test_001");

        // Java safety decision MUST REMAIN AUTHORITATIVE
        assertEquals(SuggestedAction.AUTO_REFUND_CUSTOMER, result.getRecommendedAction(), "Java recommendation cannot be overridden");
        assertTrue(result.isRetryProhibited(), "Java retry prohibition cannot be overridden");
    }

    @Test
    @DisplayName("TEST 8, 9 & 10: Successful Gemini response attached as explanation only, audit recorded, and 4 layers cleanly separated")
    void testSuccessfulGeminiResponseAttachedAndSeparated() {
        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_001")
                .paymentId("pay_test_001")
                .bankStatus(BankStatus.SUCCESS)
                .debitedAmount(BigDecimal.valueOf(4500.00))
                .utrNumber("UTR414960264709")
                .build();

        GatewayRecord gateway = GatewayRecord.builder()
                .gatewayRecordId("gw_001")
                .paymentId("pay_test_001")
                .gatewayStatus(GatewayStatus.FAILED)
                .processingLatencyMs(65000)
                .build();

        MerchantOrderRecord merchantOrder = MerchantOrderRecord.builder()
                .merchantOrderRecordId("mor_001")
                .paymentId("pay_test_001")
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(incidentCaseRepository.findById("inc_test_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_test_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(bank));
        when(gatewayRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(gateway));
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.of(merchantOrder));
        when(webhookRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_test_001")).thenReturn(Collections.emptyList());
        when(mlAssessmentRepository.findByIncidentId("inc_test_001")).thenReturn(Optional.empty());

        MlAssessmentDto ml = MlAssessmentDto.builder()
                .assessmentId("mla_001")
                .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                .confidenceScore(BigDecimal.valueOf(0.99))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(ml));

        GeminiInvestigationResponseDto mockGemini = GeminiInvestigationResponseDto.builder()
                .provider("Google Gemini")
                .modelUsed("gemini-2.5-flash")
                .summary("Customer account debited but gateway timed out.")
                .whatHappened("The customer was charged at bank switch via UPI, but gateway timed out.")
                .mlReasoning("Clear signature of BANK_DEBIT_GATEWAY_FAILURE.")
                .recommendedOperatorAction("Execute AUTO_REFUND_CUSTOMER as calculated by Java.")
                .customerImpact("Funds deducted without order fulfillment.")
                .confidenceExplanation("99% statistical confidence due to binary contradiction.")
                .build();
        when(geminiInvestigationService.explainInvestigation(any())).thenReturn(Optional.of(mockGemini));

        InvestigationResultDto result = investigationService.investigateIncident("inc_test_001");

        assertNotNull(result);

        // LAYER 1: EVIDENCE
        assertEquals("SUCCESS", result.getBankStatus());
        assertEquals("FAILED", result.getGatewayStatus());
        assertEquals("CANCELLED", result.getMerchantOrderStatus());

        // LAYER 2: ML ASSESSMENT (Random Forest)
        assertEquals("BANK_DEBIT_GATEWAY_FAILURE", result.getPredictedRootCause());
        assertEquals(BigDecimal.valueOf(0.99), result.getConfidence());

        // LAYER 3: JAVA SAFETY DECISION (Authoritative)
        assertEquals(SuggestedAction.AUTO_REFUND_CUSTOMER, result.getRecommendedAction());
        assertTrue(result.isRetryProhibited());

        // LAYER 4: GEMINI EXPLANATION (Advisory Only)
        assertNotNull(result.getGeminiExplanation());
        assertEquals("Google Gemini", result.getGeminiExplanation().getProvider());
        assertEquals("Customer account debited but gateway timed out.", result.getGeminiExplanation().getSummary());
        assertEquals("Funds deducted without order fulfillment.", result.getGeminiExplanation().getCustomerImpact());

        // TEST 9: AUDIT VERIFICATION (Recorded as advisory only, NOT financial authority)
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, atLeastOnce()).logEvent(
                eq("INCIDENT_CASES"),
                eq("inc_test_001"),
                eq("GEMINI_EXPLANATION_ATTACHED"),
                any(),
                eq("GEMINI_EXPLANATION_ASSISTANT"),
                any(),
                detailsCaptor.capture(),
                any()
        );
        assertTrue(detailsCaptor.getValue().contains("ADVISORY_EXPLANATION_ONLY"));
        assertTrue(detailsCaptor.getValue().contains("JAVA_DETERMINISTIC_RULES"));
    }
}
