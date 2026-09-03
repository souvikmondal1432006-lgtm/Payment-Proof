package com.paymentproof.service;

import com.paymentproof.dto.PagedResponseDto;
import com.paymentproof.dto.PaymentDetailDto;
import com.paymentproof.dto.PaymentDto;
import com.paymentproof.entity.BankRecord;
import com.paymentproof.entity.Payment;
import com.paymentproof.entity.enums.BankStatus;
import com.paymentproof.entity.enums.PaymentStatus;
import com.paymentproof.exception.ResourceNotFoundException;
import com.paymentproof.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

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
    private IncidentCaseRepository incidentCaseRepository;
    @Mock
    private PaymentEventRepository paymentEventRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        mockPayment = Payment.builder()
                .paymentId("pay_test_100")
                .merchantId("merch_swiggy")
                .customerId("cust_ananya")
                .orderId("ORD-5501")
                .amount(BigDecimal.valueOf(850.00))
                .currency("INR")
                .paymentMethod("CREDIT_CARD")
                .paymentMethodSubtype("HDFC_VISA_SIGNATURE")
                .status(PaymentStatus.SUCCESS)
                .clientIp("103.20.10.5")
                .userAgent("Mozilla/5.0")
                .initiatedAt(LocalDateTime.now().minusHours(2))
                .completedAt(LocalDateTime.now().minusHours(2).plusSeconds(3))
                .updatedAt(LocalDateTime.now().minusHours(2).plusSeconds(3))
                .build();
    }

    @Test
    @DisplayName("Retrieve paginated payments with filter criteria")
    void testGetPaymentsWithFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(mockPayment), pageable, 1);

        when(paymentRepository.findWithFilters(eq("merch_swiggy"), eq(PaymentStatus.SUCCESS), any(), any(), eq(pageable)))
                .thenReturn(page);

        PagedResponseDto<PaymentDto> result = paymentService.getPayments("merch_swiggy", PaymentStatus.SUCCESS, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("pay_test_100", result.getContent().get(0).getPaymentId());
        assertEquals("merch_swiggy", result.getContent().get(0).getMerchantId());
    }

    @Test
    @DisplayName("Retrieve aggregated payment details across multi-party records")
    void testGetPaymentDetail() {
        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_100")
                .paymentId("pay_test_100")
                .bankName("HDFC_BANK")
                .bankStatus(BankStatus.SUCCESS)
                .utrNumber("UTR88776655")
                .debitedAmount(BigDecimal.valueOf(850.00))
                .bankTimestamp(LocalDateTime.now().minusHours(2))
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(paymentRepository.findById("pay_test_100")).thenReturn(Optional.of(mockPayment));
        when(bankRecordRepository.findByPaymentId("pay_test_100")).thenReturn(Optional.of(bank));
        when(gatewayRecordRepository.findByPaymentId("pay_test_100")).thenReturn(Optional.empty());
        when(merchantOrderRecordRepository.findByPaymentId("pay_test_100")).thenReturn(Optional.empty());
        when(webhookRecordRepository.findByPaymentId("pay_test_100")).thenReturn(Optional.empty());
        when(settlementRecordRepository.findByPaymentId("pay_test_100")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_test_100")).thenReturn(Optional.empty());
        when(incidentCaseRepository.findFirstByPaymentIdOrderByOpenedAtDesc("pay_test_100")).thenReturn(Optional.empty());
        when(paymentEventRepository.findByPaymentIdOrderByEventTimestampAsc("pay_test_100")).thenReturn(Collections.emptyList());

        PaymentDetailDto detail = paymentService.getPaymentDetail("pay_test_100");

        assertNotNull(detail);
        assertEquals("pay_test_100", detail.getPayment().getPaymentId());
        assertNotNull(detail.getBankRecord());
        assertEquals("UTR88776655", detail.getBankRecord().getUtrNumber());
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException when payment not found")
    void testGetPaymentNotFound() {
        when(paymentRepository.findById("invalid_id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentById("invalid_id"));
    }
}
