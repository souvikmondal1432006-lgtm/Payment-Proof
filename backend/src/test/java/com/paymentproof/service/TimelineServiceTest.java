package com.paymentproof.service;

import com.paymentproof.dto.TimelineEventDto;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimelineServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentEventRepository paymentEventRepository;
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
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private TimelineService timelineService;

    private Payment mockPayment;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now().minusHours(1);
        mockPayment = Payment.builder()
                .paymentId("pay_timeline_01")
                .merchantId("merch_zomato")
                .customerId("cust_vikram")
                .orderId("ORD-8801")
                .amount(BigDecimal.valueOf(1200.00))
                .currency("INR")
                .paymentMethod("UPI")
                .status(PaymentStatus.SUCCESS)
                .initiatedAt(baseTime)
                .build();
    }

    @Test
    @DisplayName("Synthesize unified chronological multi-system timeline")
    void testGetTimelineForPayment() {
        PaymentEvent pe = PaymentEvent.builder()
                .eventId("pevt_01")
                .paymentId("pay_timeline_01")
                .eventType("AUTHENTICATION_REQUESTED")
                .fromStatus("INITIATED")
                .toStatus("PENDING")
                .eventSource("GATEWAY_ENGINE")
                .eventTimestamp(baseTime.plusSeconds(1))
                .build();

        BankRecord bank = BankRecord.builder()
                .bankRecordId("bnk_01")
                .paymentId("pay_timeline_01")
                .bankName("ICICI_BANK")
                .bankStatus(BankStatus.SUCCESS)
                .utrNumber("UTR11223344")
                .debitedAmount(BigDecimal.valueOf(1200.00))
                .bankTimestamp(baseTime.plusSeconds(2))
                .createdAt(baseTime.plusSeconds(2))
                .build();

        GatewayRecord gateway = GatewayRecord.builder()
                .gatewayRecordId("gw_01")
                .paymentId("pay_timeline_01")
                .gatewayName("RAZORPAY")
                .gatewayStatus(GatewayStatus.SUCCESS)
                .authStatus(AuthStatus.AUTHORIZED)
                .captureStatus(CaptureStatus.CAPTURED)
                .fee(BigDecimal.valueOf(24.00))
                .gatewayTimestamp(baseTime.plusSeconds(3))
                .createdAt(baseTime.plusSeconds(3))
                .build();

        WebhookRecord webhook = WebhookRecord.builder()
                .webhookId("wh_01")
                .paymentId("pay_timeline_01")
                .merchantId("merch_zomato")
                .eventName("payment.captured")
                .targetUrl("https://api.zomato.com/callback")
                .deliveryStatus(WebhookDeliveryStatus.DELIVERED)
                .httpStatusCode(200)
                .attemptCount(1)
                .firstAttemptAt(baseTime.plusSeconds(4))
                .createdAt(baseTime.plusSeconds(4))
                .build();

        when(paymentRepository.findById("pay_timeline_01")).thenReturn(Optional.of(mockPayment));
        when(paymentEventRepository.findByPaymentIdOrderByEventTimestampAsc("pay_timeline_01")).thenReturn(List.of(pe));
        when(bankRecordRepository.findByPaymentId("pay_timeline_01")).thenReturn(Optional.of(bank));
        when(gatewayRecordRepository.findByPaymentId("pay_timeline_01")).thenReturn(Optional.of(gateway));
        when(merchantOrderRecordRepository.findByPaymentId("pay_timeline_01")).thenReturn(Optional.empty());
        when(webhookRecordRepository.findByPaymentId("pay_timeline_01")).thenReturn(Optional.of(webhook));
        when(settlementRecordRepository.findByPaymentId("pay_timeline_01")).thenReturn(Optional.empty());
        when(refundRecordRepository.findByPaymentId("pay_timeline_01")).thenReturn(Optional.empty());

        List<TimelineEventDto> timeline = timelineService.getTimelineForPayment("pay_timeline_01");

        assertNotNull(timeline);
        assertTrue(timeline.size() >= 4, "Timeline must contain initialization, payment event, bank telemetry, gateway telemetry, and webhook dispatch");

        // Verify strictly chronological ordering
        for (int i = 0; i < timeline.size() - 1; i++) {
            assertTrue(!timeline.get(i).getTimestamp().isAfter(timeline.get(i + 1).getTimestamp()),
                    "Timeline events must be strictly chronological");
        }
    }
}
