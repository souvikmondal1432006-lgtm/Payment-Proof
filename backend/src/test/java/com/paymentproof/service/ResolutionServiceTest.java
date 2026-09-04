package com.paymentproof.service;

import com.paymentproof.dto.ResolutionDto;
import com.paymentproof.dto.ResolutionRequestDto;
import com.paymentproof.entity.BankRecord;
import com.paymentproof.entity.IncidentCase;
import com.paymentproof.entity.Payment;
import com.paymentproof.entity.Resolution;
import com.paymentproof.entity.enums.*;
import com.paymentproof.exception.InvalidOperationException;
import com.paymentproof.repository.BankRecordRepository;
import com.paymentproof.repository.IncidentCaseRepository;
import com.paymentproof.repository.PaymentRepository;
import com.paymentproof.repository.ResolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolutionServiceTest {

    @Mock
    private ResolutionRepository resolutionRepository;
    @Mock
    private IncidentCaseRepository incidentCaseRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BankRecordRepository bankRecordRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ResolutionService resolutionService;

    private Payment mockPayment;
    private IncidentCase mockIncident;

    @BeforeEach
    void setUp() {
        mockPayment = Payment.builder()
                .paymentId("pay_res_01")
                .merchantId("merch_flipkart")
                .amount(BigDecimal.valueOf(3200.00))
                .status(PaymentStatus.FLAGGED)
                .build();

        mockIncident = IncidentCase.builder()
                .incidentId("inc_res_01")
                .paymentId("pay_res_01")
                .incidentType(IncidentType.BANK_DEBIT_GATEWAY_FAILURE)
                .caseStatus(CaseStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("Apply customer refund resolution and update payment status to REFUNDED")
    void testResolveIncidentCustomerRefund() {
        ResolutionRequestDto request = ResolutionRequestDto.builder()
                .actionTaken(ResolutionAction.CUSTOMER_REFUNDED)
                .resolutionType(ResolutionType.ML_SUPERVISED_AUTO)
                .resolvedBy("SYSTEM_AUTO_RECON")
                .resolutionNotes("Customer auto-refunded due to ghost debit")
                .liabilityParty(LiabilityParty.GATEWAY)
                .financialImpactAmount(BigDecimal.valueOf(3200.00))
                .build();

        when(incidentCaseRepository.findById("inc_res_01")).thenReturn(Optional.of(mockIncident));
        when(paymentRepository.findById("pay_res_01")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_res_01")).thenReturn(Optional.empty());
        when(resolutionRepository.findByIncidentId("inc_res_01")).thenReturn(Optional.empty());
        when(resolutionRepository.save(any(Resolution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResolutionDto result = resolutionService.resolveIncident("inc_res_01", request);

        assertNotNull(result);
        assertEquals(ResolutionAction.CUSTOMER_REFUNDED, result.getActionTaken());
        assertEquals(CaseStatus.RESOLVED, mockIncident.getCaseStatus());
        assertEquals(PaymentStatus.REFUNDED, mockPayment.getStatus());
        assertEquals(BigDecimal.valueOf(3200.00), result.getFinancialImpactAmount());

        verify(resolutionRepository, times(1)).save(any(Resolution.class));
        verify(incidentCaseRepository, times(1)).save(mockIncident);
        verify(paymentRepository, times(1)).save(mockPayment);
        verify(auditService, times(1)).logEvent(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Safety Invariant 1: External AI models (Gemini / ML) cannot authorize or execute resolutions")
    void testResolveIncident_UnauthorizedActor_Rejected() {
        ResolutionRequestDto geminiRequest = ResolutionRequestDto.builder()
                .actionTaken(ResolutionAction.CUSTOMER_REFUNDED)
                .resolutionType(ResolutionType.AUTOMATED_RULE_ENGINE)
                .resolvedBy("GEMINI_EXPLANATION_ASSISTANT")
                .build();

        InvalidOperationException ex1 = assertThrows(InvalidOperationException.class,
                () -> resolutionService.resolveIncident("inc_res_01", geminiRequest));
        assertTrue(ex1.getMessage().contains("External advisory models"));

        ResolutionRequestDto mlRequest = ResolutionRequestDto.builder()
                .actionTaken(ResolutionAction.CUSTOMER_REFUNDED)
                .resolutionType(ResolutionType.AUTOMATED_RULE_ENGINE)
                .resolvedBy("PYTHON_ML_SERVICE")
                .build();

        InvalidOperationException ex2 = assertThrows(InvalidOperationException.class,
                () -> resolutionService.resolveIncident("inc_res_01", mlRequest));
        assertTrue(ex2.getMessage().contains("External advisory models"));
    }

    @Test
    @DisplayName("Safety Invariant 2: Incidents under NEEDS_REVIEW prohibit automated resolution; require OPERATOR_MANUAL_OVERRIDE")
    void testResolveIncident_NeedsReview_RequiresManualOverride() {
        mockIncident.setCaseStatus(CaseStatus.NEEDS_REVIEW);
        when(incidentCaseRepository.findById("inc_res_01")).thenReturn(Optional.of(mockIncident));

        ResolutionRequestDto autoRequest = ResolutionRequestDto.builder()
                .actionTaken(ResolutionAction.CUSTOMER_REFUNDED)
                .resolutionType(ResolutionType.AUTOMATED_RULE_ENGINE)
                .resolvedBy("SYSTEM_AUTO_RECON")
                .build();

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> resolutionService.resolveIncident("inc_res_01", autoRequest));
        assertTrue(ex.getMessage().contains("NEEDS_REVIEW"));
    }

    @Test
    @DisplayName("Safety Invariant 3: Active bank debit prohibits closing incident as NO_DISCREPANCY_FOUND without remediation")
    void testResolveIncident_ActiveDebit_ProhibitsNoDiscrepancy() {
        when(incidentCaseRepository.findById("inc_res_01")).thenReturn(Optional.of(mockIncident));

        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_01")
                .paymentId("pay_res_01")
                .bankStatus(BankStatus.SUCCESS)
                .utrNumber("414960264709")
                .build();
        when(bankRecordRepository.findByPaymentId("pay_res_01")).thenReturn(Optional.of(bank));

        ResolutionRequestDto unsafeRequest = ResolutionRequestDto.builder()
                .actionTaken(ResolutionAction.NO_DISCREPANCY_FOUND)
                .resolutionType(ResolutionType.OPERATOR_MANUAL_OVERRIDE)
                .resolvedBy("operator_priya_m")
                .build();

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> resolutionService.resolveIncident("inc_res_01", unsafeRequest));
        assertTrue(ex.getMessage().contains("STRICT SAFETY INVARIANT VIOLATION"));
    }
}
