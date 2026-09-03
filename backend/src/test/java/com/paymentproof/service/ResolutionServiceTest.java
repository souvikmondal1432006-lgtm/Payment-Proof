package com.paymentproof.service;

import com.paymentproof.dto.ResolutionDto;
import com.paymentproof.dto.ResolutionRequestDto;
import com.paymentproof.entity.AuditEvent;
import com.paymentproof.entity.IncidentCase;
import com.paymentproof.entity.Payment;
import com.paymentproof.entity.Resolution;
import com.paymentproof.entity.enums.*;
import com.paymentproof.repository.AuditEventRepository;
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
}
