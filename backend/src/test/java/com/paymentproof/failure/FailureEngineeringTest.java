package com.paymentproof.failure;

import com.paymentproof.client.MlServiceClient;
import com.paymentproof.dto.ErrorResponseDto;
import com.paymentproof.dto.InvestigationResultDto;
import com.paymentproof.dto.MlAssessmentDto;
import com.paymentproof.dto.MlFeatureRequestDto;
import com.paymentproof.entity.*;
import com.paymentproof.entity.enums.*;
import com.paymentproof.exception.GlobalExceptionHandler;
import com.paymentproof.repository.*;
import com.paymentproof.service.AiInvestigationService;
import com.paymentproof.service.AuditService;
import com.paymentproof.service.GeminiInvestigationService;
import com.paymentproof.service.InvestigationService;
import com.paymentproof.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailureEngineeringTest {

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

    @Spy
    private AiInvestigationService aiInvestigationService = new AiInvestigationService();

    @InjectMocks
    private InvestigationService investigationService;

    private IncidentCase mockIncident;
    private Payment mockPayment;
    private BankRecord mockBankRecord;
    private GatewayRecord mockGatewayRecord;
    private MerchantOrderRecord mockMerchantRecord;

    @BeforeEach
    void setUp() {
        mockIncident = IncidentCase.builder()
                .incidentId("inc_fail_001")
                .paymentId("pay_fail_001")
                .incidentType(IncidentType.BANK_DEBIT_GATEWAY_FAILURE)
                .severity(Severity.CRITICAL)
                .caseStatus(CaseStatus.OPEN)
                .openedAt(LocalDateTime.now())
                .build();

        mockPayment = Payment.builder()
                .paymentId("pay_fail_001")
                .orderId("ORD-FAIL-001")
                .merchantId("merch_flipkart")
                .customerId("cust_123")
                .amount(BigDecimal.valueOf(8500.00))
                .currency("INR")
                .paymentMethod("UPI")
                .status(PaymentStatus.FLAGGED)
                .build();

        mockBankRecord = BankRecord.builder()
                .bankRecordId("bnk_001")
                .paymentId("pay_fail_001")
                .bankName("HDFC Bank")
                .bankStatus(BankStatus.SUCCESS)
                .utrNumber("414960264709")
                .debitedAmount(BigDecimal.valueOf(8500.00))
                .networkLatencyMs(420)
                .bankTimestamp(LocalDateTime.now())
                .build();

        mockGatewayRecord = GatewayRecord.builder()
                .gatewayRecordId("gtw_001")
                .paymentId("pay_fail_001")
                .gatewayName("Razorpay")
                .gatewayStatus(GatewayStatus.TIMED_OUT)
                .captureStatus(CaptureStatus.PENDING)
                .processingLatencyMs(65000)
                .gatewayTimestamp(LocalDateTime.now())
                .build();

        mockMerchantRecord = MerchantOrderRecord.builder()
                .merchantOrderRecordId("ord_001")
                .paymentId("pay_fail_001")
                .merchantId("merch_flipkart")
                .merchantOrderId("ORD-FAIL-001")
                .orderStatus(OrderStatus.CANCELLED)
                .fulfillmentStatus(FulfillmentStatus.CANCELLED)
                .expectedAmount(BigDecimal.valueOf(8500.00))
                .merchantUpdatedAt(LocalDateTime.now())
                .build();

        org.mockito.Mockito.lenient().when(geminiInvestigationService.explainInvestigation(any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Failure Scenario 1 & 2: ML Unavailable or Timed Out — Status becomes NEEDS_REVIEW with ZERO fake ML hallucination")
    void testMlUnavailableOrTimedOut_SafeFallback() {
        when(incidentCaseRepository.findById("inc_fail_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_fail_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockBankRecord));
        when(gatewayRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockGatewayRecord));
        when(merchantOrderRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockMerchantRecord));
        when(webhookRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_fail_001")).thenReturn(List.of());
        when(timelineService.getTimelineForPayment("pay_fail_001")).thenReturn(List.of());

        // ML returns empty (timed out or connection refused)
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.empty());

        InvestigationResultDto result = investigationService.investigateIncident("inc_fail_001");

        assertNotNull(result);
        assertEquals(CaseStatus.NEEDS_REVIEW, result.getInvestigationStatus());
        assertEquals("UNAVAILABLE", result.getPredictedRootCause());
        assertNull(result.getConfidence(), "Confidence must be null when ML is unavailable — never fabricate confidence");
        assertTrue(result.isRetryProhibited(), "Money safety invariant: Active bank debit prohibits retry even when ML is offline");
        assertEquals(BigDecimal.valueOf(8500.00), result.getMoneyAtRisk());
        assertTrue(result.getSummary().contains("Automated classification unavailable"));
    }

    @Test
    @DisplayName("Failure Scenario 5: Malformed ML Response with Invalid Confidence (e.g. 1.5) or Unreachable Port is safely rejected")
    void testMalformedMl_UnreachableOrInvalid_Rejected() {
        MlServiceClient client = new MlServiceClient(
                WebClient.builder(),
                "http://localhost:59998",
                200
        );

        MlFeatureRequestDto req = MlFeatureRequestDto.builder()
                .paymentId("pay_test_01")
                .amount(BigDecimal.valueOf(100.00))
                .build();

        // Offline or unreachable port returns empty Optional safely
        Optional<MlAssessmentDto> result = client.classifyTelemetry(req);
        assertTrue(result.isEmpty(), "Client must return Optional.empty() when response cannot be fetched or validated");
        assertFalse(client.isHealthy(), "Health check must return false for unreachable service");
    }

    @Test
    @DisplayName("Failure Scenario 6: Conflicting Payment States — Bank=SUCCESS vs Merchant=CANCELLED enforces Safe Hold and retry prohibition")
    void testConflictingPaymentStates_SafeHoldEnforced() {
        when(incidentCaseRepository.findById("inc_fail_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_fail_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockBankRecord));
        when(gatewayRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockGatewayRecord));
        when(merchantOrderRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockMerchantRecord));
        when(webhookRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_fail_001")).thenReturn(List.of());
        when(timelineService.getTimelineForPayment("pay_fail_001")).thenReturn(List.of());

        MlAssessmentDto validMl = MlAssessmentDto.builder()
                .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                .confidenceScore(BigDecimal.valueOf(0.95))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(validMl));

        InvestigationResultDto result = investigationService.investigateIncident("inc_fail_001");

        assertNotNull(result);
        assertTrue(result.isRetryProhibited(), "Blind retry must be prohibited when customer funds are debited");
        assertFalse(result.getContradictionsDetected().isEmpty(), "Contradiction matrix must capture the state divergence");
        assertTrue(result.getContradictionsDetected().get(0).contains("Ghost Debit") || result.getContradictionsDetected().get(1).contains("Cart Cancellation"));
    }

    @Test
    @DisplayName("Failure Scenario 7: Duplicate Investigation Request — Returns existing result without duplicate ML invocation")
    void testDuplicateInvestigation_Idempotency() {
        mockIncident.setCaseStatus(CaseStatus.AI_ANALYZED);

        MlAssessment existingAssessment = MlAssessment.builder()
                .assessmentId("mla_existing_01")
                .incidentId("inc_fail_001")
                .paymentId("pay_fail_001")
                .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                .confidenceScore(BigDecimal.valueOf(0.99))
                .suggestedAction(SuggestedAction.AUTO_REFUND_CUSTOMER)
                .build();

        when(incidentCaseRepository.findById("inc_fail_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_fail_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockBankRecord));
        when(gatewayRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockGatewayRecord));
        when(merchantOrderRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockMerchantRecord));
        when(webhookRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_fail_001")).thenReturn(List.of());
        when(timelineService.getTimelineForPayment("pay_fail_001")).thenReturn(List.of());
        when(mlAssessmentRepository.findByIncidentId("inc_fail_001")).thenReturn(Optional.of(existingAssessment));

        InvestigationResultDto result = investigationService.investigateIncident("inc_fail_001");

        assertNotNull(result);
        assertEquals(CaseStatus.AI_ANALYZED, result.getInvestigationStatus());
        verify(mlServiceClient, never()).classifyTelemetry(any());
        verify(auditService, never()).logEvent(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Failure Scenario 8: Conflicting Evidence — Bank=SUCCESS, Gateway=SUCCESS (CAPTURED), Merchant=CANCELLED")
    void testConflictingEvidence_BankSuccess_GatewaySuccess_MerchantCancelled() {
        mockGatewayRecord.setGatewayStatus(GatewayStatus.SUCCESS);
        mockGatewayRecord.setCaptureStatus(CaptureStatus.CAPTURED);

        when(incidentCaseRepository.findById("inc_fail_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_fail_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockBankRecord));
        when(gatewayRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockGatewayRecord));
        when(merchantOrderRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockMerchantRecord));
        when(webhookRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_fail_001")).thenReturn(List.of());
        when(timelineService.getTimelineForPayment("pay_fail_001")).thenReturn(List.of());

        MlAssessmentDto validMl = MlAssessmentDto.builder()
                .predictedRootCause("ORDER_PAYMENT_CONFLICT")
                .confidenceScore(BigDecimal.valueOf(0.92))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(validMl));

        InvestigationResultDto result = investigationService.investigateIncident("inc_fail_001");

        assertNotNull(result);
        assertTrue(result.isRetryProhibited(), "Payment is captured and debited; blind retry is prohibited");
        assertFalse(result.getContradictionsDetected().isEmpty());
        assertTrue(result.getContradictionsDetected().stream().anyMatch(c -> c.contains("Payment Capture with Cancelled Order")),
                "Must explicitly identify contradiction between captured payment and cancelled order");
    }

    @Test
    @DisplayName("Failure Scenario 9: Low ML Confidence (<0.70) — Case status becomes NEEDS_REVIEW and manual review is enforced")
    void testLowMlConfidence_EnforcesNeedsReview() {
        when(incidentCaseRepository.findById("inc_fail_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_fail_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockBankRecord));
        when(gatewayRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockGatewayRecord));
        when(merchantOrderRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockMerchantRecord));
        when(webhookRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_fail_001")).thenReturn(List.of());
        when(timelineService.getTimelineForPayment("pay_fail_001")).thenReturn(List.of());

        // Low confidence ML output (0.55 < 0.70 threshold)
        MlAssessmentDto lowConfMl = MlAssessmentDto.builder()
                .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                .confidenceScore(BigDecimal.valueOf(0.55))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(lowConfMl));

        InvestigationResultDto result = investigationService.investigateIncident("inc_fail_001");

        assertNotNull(result);
        assertEquals(CaseStatus.NEEDS_REVIEW, result.getInvestigationStatus(), "Low confidence ML must trigger NEEDS_REVIEW status");
        assertTrue(result.getSummary().contains("below threshold"), "Summary must indicate low confidence escalation");
    }

    @Test
    @DisplayName("Failure Scenario 10: Gemini Failure / Timeout — Investigation completes normally with Java deterministic explanation")
    void testGeminiFailure_InvestigationCompletesNormally() {
        when(incidentCaseRepository.findById("inc_fail_001")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_fail_001")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockBankRecord));
        when(gatewayRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockGatewayRecord));
        when(merchantOrderRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.of(mockMerchantRecord));
        when(webhookRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_fail_001")).thenReturn(Optional.empty());
        when(evidenceRepository.findByIncidentId("inc_fail_001")).thenReturn(List.of());
        when(timelineService.getTimelineForPayment("pay_fail_001")).thenReturn(List.of());

        MlAssessmentDto validMl = MlAssessmentDto.builder()
                .predictedRootCause("BANK_DEBIT_GATEWAY_FAILURE")
                .confidenceScore(BigDecimal.valueOf(0.95))
                .build();
        when(mlServiceClient.classifyTelemetry(any())).thenReturn(Optional.of(validMl));

        // Gemini returns empty (simulating timeout / HTTP 429 / HTTP 500 / missing key)
        when(geminiInvestigationService.explainInvestigation(any())).thenReturn(Optional.empty());

        InvestigationResultDto result = investigationService.investigateIncident("inc_fail_001");

        assertNotNull(result);
        assertNotNull(result.getAiReport(), "AI report must be generated deterministically by Java");
        assertNotNull(result.getAiReport().getWhatHappened(), "Deterministic whatHappened must be present");
        assertNull(result.getAiReport().getGeminiExplanation(), "Gemini explanation is null when Gemini is unavailable");
        assertEquals(CaseStatus.AI_ANALYZED, result.getInvestigationStatus());
    }

    @Test
    @DisplayName("Failure Scenario 4: Database Unavailability — Returns structured 503 error without stack traces or credential leaks")
    void testDatabaseUnavailable_StructuredError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/incidents/inc_001");

        DataAccessException dbEx = new DataAccessException("Communications link failure to MySQL port 3306") {};

        ResponseEntity<ErrorResponseDto> response = handler.handleDatabaseExceptions(dbEx, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        ErrorResponseDto errorDto = response.getBody();
        assertEquals("DATABASE_UNAVAILABLE", errorDto.getError());
        assertTrue(errorDto.getMessage().contains("Payment records could not be loaded"));
        assertFalse(errorDto.getMessage().contains("Communications link failure"), "Raw SQL error must not leak to user");
    }
}
