package com.paymentproof.service;

import com.paymentproof.dto.IncidentCaseDto;
import com.paymentproof.entity.*;
import com.paymentproof.entity.enums.*;
import com.paymentproof.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeroIncidentService {

    public static final String HERO_INCIDENT_ID = "inc_test_001";
    public static final String HERO_PAYMENT_ID = "pay_test_001";
    public static final String HERO_ORDER_ID = "ORD-2026-TEST01";
    public static final String HERO_MERCHANT_ID = "merch_swiggy_ind";
    public static final BigDecimal HERO_AMOUNT = new BigDecimal("4500.00");
    public static final String HERO_UTR = "UTR984102947101";

    private final PaymentRepository paymentRepository;
    private final BankRecordRepository bankRecordRepository;
    private final GatewayRecordRepository gatewayRecordRepository;
    private final MerchantOrderRecordRepository merchantOrderRecordRepository;
    private final WebhookRecordRepository webhookRecordRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final IncidentCaseRepository incidentCaseRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final InvestigationEvidenceRepository investigationEvidenceRepository;
    private final MlAssessmentRepository mlAssessmentRepository;
    private final ResolutionRepository resolutionRepository;

    @Transactional
    public synchronized void seedHeroIncident() {
        if (incidentCaseRepository.existsById(HERO_INCIDENT_ID)) {
            log.info("Hero demo incident {} already exists. Preserving current state.", HERO_INCIDENT_ID);
            return;
        }

        log.info("Seeding authoritative hero demo incident: {} (Payment: {})", HERO_INCIDENT_ID, HERO_PAYMENT_ID);
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(5);

        // 1. Payment
        Payment payment = Payment.builder()
                .paymentId(HERO_PAYMENT_ID)
                .merchantId(HERO_MERCHANT_ID)
                .customerId("cust_aarav_sharma_981")
                .orderId(HERO_ORDER_ID)
                .amount(HERO_AMOUNT)
                .currency("INR")
                .paymentMethod("UPI")
                .paymentMethodSubtype("PHONEPE")
                .status(PaymentStatus.FAILED)
                .clientIp("103.21.14.88")
                .userAgent("Mozilla/5.0 (Linux; Android 14) Chrome/128.0.0.0")
                .initiatedAt(baseTime)
                .updatedAt(baseTime.plusSeconds(70))
                .build();
        paymentRepository.save(payment);

        // 2. Bank Record: SUCCESS, debited = 4500.00, UTR present, latency = 420 ms
        BankRecord bankRecord = BankRecord.builder()
                .bankRecordId("bnk_test_001")
                .paymentId(HERO_PAYMENT_ID)
                .bankName("HDFC_BANK")
                .bankReferenceNumber("REF-HDFC-9912048")
                .utrNumber(HERO_UTR)
                .accountLast4("4821")
                .bankStatus(BankStatus.SUCCESS)
                .debitedAmount(HERO_AMOUNT)
                .currency("INR")
                .responseCode("00")
                .responseMessage("DEBIT_APPROVED_SUCCESS")
                .networkLatencyMs(420)
                .bankTimestamp(baseTime.plusNanos(420_000_000))
                .rawPayload("{\"cbs_node\": \"cbs-hdfc-prod-01\", \"npci_rrn\": \"429184029471\", \"auth_code\": \"AUTH99128\", \"response_code\": \"00\", \"utr\": \"" + HERO_UTR + "\"}")
                .build();
        bankRecordRepository.save(bankRecord);

        // 3. Gateway Record: FAILED, Auth = FAILED, Capture = NOT_REQUESTED, Latency = 65 seconds
        GatewayRecord gatewayRecord = GatewayRecord.builder()
                .gatewayRecordId("gw_test_001")
                .paymentId(HERO_PAYMENT_ID)
                .gatewayName("RAZORPAY")
                .gatewayTransactionId("txn_rzp_test_001")
                .gatewayOrderId("order_rzp_test_001")
                .authStatus(AuthStatus.FAILED)
                .captureStatus(CaptureStatus.NOT_REQUESTED)
                .gatewayStatus(GatewayStatus.FAILED)
                .authorizedAmount(BigDecimal.ZERO)
                .capturedAmount(BigDecimal.ZERO)
                .fee(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .errorCode("GATEWAY_TIMEOUT_POST_DEBIT")
                .errorDescription("Downstream PSP connection timed out after 65,000ms. No capture confirmation received.")
                .processingLatencyMs(65000)
                .gatewayTimestamp(baseTime.plusSeconds(65))
                .rawPayload("{\"engine\": \"RAZORPAY\", \"error_code\": \"GATEWAY_TIMEOUT\", \"timeout_ms\": 65000, \"stage\": \"CAPTURE_WAIT\", \"risk_verdict\": \"CLEAN\"}")
                .build();
        gatewayRecordRepository.save(gatewayRecord);

        // 4. Merchant Order: CANCELLED, Fulfilled = CANCELLED / FALSE
        MerchantOrderRecord merchantOrder = MerchantOrderRecord.builder()
                .merchantOrderRecordId("mor_test_001")
                .paymentId(HERO_PAYMENT_ID)
                .merchantId(HERO_MERCHANT_ID)
                .merchantOrderId(HERO_ORDER_ID)
                .orderStatus(OrderStatus.CANCELLED)
                .fulfillmentStatus(FulfillmentStatus.CANCELLED)
                .expectedAmount(HERO_AMOUNT)
                .currency("INR")
                .cancellationReason("SESSION_TIMEOUT_NO_PROOF")
                .customerNotes("Cart reservation released automatically due to absence of gateway confirmation webhook.")
                .merchantUpdatedAt(baseTime.plusSeconds(70))
                .build();
        merchantOrderRecordRepository.save(merchantOrder);

        // 5. Webhook Record: DROPPED, HTTP 504
        WebhookRecord webhookRecord = WebhookRecord.builder()
                .webhookId("wh_test_001")
                .paymentId(HERO_PAYMENT_ID)
                .merchantId(HERO_MERCHANT_ID)
                .eventName("payment.failed")
                .targetUrl("https://api.swiggy.com/payments/webhook")
                .attemptCount(3)
                .maxAttempts(3)
                .deliveryStatus(WebhookDeliveryStatus.DROPPED)
                .httpStatusCode(504)
                .latencyMs(5000)
                .requestPayloadHash("a7f9b8c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0")
                .requestPayload("{\"event\": \"payment.failed\", \"payment_id\": \"" + HERO_PAYMENT_ID + "\", \"error\": \"TIMEOUT_504\"}")
                .responseBody("{\"error\": \"Gateway Timeout 504\"}")
                .firstAttemptAt(baseTime.plusSeconds(66))
                .lastAttemptAt(baseTime.plusSeconds(75))
                .build();
        webhookRecordRepository.save(webhookRecord);

        // 6. Settlement & Refund: Explicitly NOT FOUND / NULL
        settlementRecordRepository.findByPaymentId(HERO_PAYMENT_ID).ifPresent(settlementRecordRepository::delete);
        refundRecordRepository.findByPaymentId(HERO_PAYMENT_ID).ifPresent(refundRecordRepository::delete);

        // 7. Incident Case
        IncidentCase incidentCase = IncidentCase.builder()
                .incidentId(HERO_INCIDENT_ID)
                .paymentId(HERO_PAYMENT_ID)
                .incidentType(IncidentType.BANK_DEBIT_GATEWAY_FAILURE)
                .severity(Severity.CRITICAL)
                .caseStatus(CaseStatus.OPEN)
                .triggerSource(TriggerSource.AUTOMATED_RECONCILIATION)
                .assignedInvestigator("operator_priya_m")
                .title("Critical Ghost Debit Detected on " + HERO_ORDER_ID)
                .description("Customer was debited ₹4,500 by bank switch with confirmed UTR, but gateway timed out and failed before capture, merchant OMS cancelled order, and webhook was dropped.")
                .openedAt(baseTime.plusSeconds(80))
                .updatedAt(baseTime.plusSeconds(80))
                .build();
        incidentCaseRepository.save(incidentCase);

        // 8. Payment Lifecycle Events
        paymentEventRepository.save(PaymentEvent.builder()
                .eventId("pevt_test_001")
                .paymentId(HERO_PAYMENT_ID)
                .eventType("PAYMENT_INITIATED")
                .fromStatus(null)
                .toStatus("INITIATED")
                .eventSource("CLIENT_SDK")
                .eventPayload("{\"client_ip\": \"103.21.14.88\", \"app_version\": \"v4.9.1\"}")
                .eventTimestamp(baseTime)
                .build());

        paymentEventRepository.save(PaymentEvent.builder()
                .eventId("pevt_test_002")
                .paymentId(HERO_PAYMENT_ID)
                .eventType("BANK_DEBIT_ACKNOWLEDGED")
                .fromStatus("INITIATED")
                .toStatus("PENDING")
                .eventSource("BANK_CONNECTOR")
                .eventPayload("{\"bank\": \"HDFC_BANK\", \"utr\": \"" + HERO_UTR + "\", \"amount\": 4500.00, \"code\": \"00\"}")
                .eventTimestamp(baseTime.plusNanos(420_000_000))
                .build());

        paymentEventRepository.save(PaymentEvent.builder()
                .eventId("pevt_test_003")
                .paymentId(HERO_PAYMENT_ID)
                .eventType("GATEWAY_TIMEOUT_OCCURRED")
                .fromStatus("PENDING")
                .toStatus("FAILED")
                .eventSource("GATEWAY_ENGINE")
                .eventPayload("{\"gateway\": \"RAZORPAY\", \"error\": \"GATEWAY_TIMEOUT_POST_DEBIT\", \"latency_ms\": 65000}")
                .eventTimestamp(baseTime.plusSeconds(65))
                .build());

        paymentEventRepository.save(PaymentEvent.builder()
                .eventId("pevt_test_004")
                .paymentId(HERO_PAYMENT_ID)
                .eventType("MERCHANT_CART_CANCELLED")
                .fromStatus("PENDING")
                .toStatus("CANCELLED")
                .eventSource("MERCHANT_OMS")
                .eventPayload("{\"reason\": \"SESSION_TIMEOUT_NO_PROOF\", \"order_id\": \"" + HERO_ORDER_ID + "\"}")
                .eventTimestamp(baseTime.plusSeconds(70))
                .build());

        paymentEventRepository.save(PaymentEvent.builder()
                .eventId("pevt_test_005")
                .paymentId(HERO_PAYMENT_ID)
                .eventType("WEBHOOK_DELIVERY_FAILED")
                .fromStatus("SCHEDULED")
                .toStatus("DROPPED")
                .eventSource("WEBHOOK_BROKER")
                .eventPayload("{\"http_code\": 504, \"attempt\": 3, \"status\": \"DROPPED\"}")
                .eventTimestamp(baseTime.plusSeconds(75))
                .build());

        // 9. Raw Evidence Items
        investigationEvidenceRepository.save(InvestigationEvidence.builder()
                .evidenceId("evi_test_001")
                .incidentId(HERO_INCIDENT_ID)
                .paymentId(HERO_PAYMENT_ID)
                .evidenceSource(EvidenceSource.CORE_BANKING_LOG)
                .evidenceType(EvidenceType.JSON_TELEMETRY)
                .filePath("/telemetry/bank/hdfc/pay_test_001.json")
                .rawContent("{\"cbs_status\": \"SUCCESS\", \"utr\": \"" + HERO_UTR + "\", \"amount\": 4500.00, \"response_code\": \"00\", \"latency_ms\": 420}")
                .payloadChecksum("9f82d1a4b6c8e0f2a4b6c8d0e2f4a6b8c0d2e4f6a8b0c2d4e6f8a0b2c4d6e8f0")
                .capturedAt(baseTime.plusNanos(420_000_000))
                .build());

        investigationEvidenceRepository.save(InvestigationEvidence.builder()
                .evidenceId("evi_test_002")
                .incidentId(HERO_INCIDENT_ID)
                .paymentId(HERO_PAYMENT_ID)
                .evidenceSource(EvidenceSource.GATEWAY_TELEMETRY)
                .evidenceType(EvidenceType.JSON_TELEMETRY)
                .filePath("/telemetry/gateway/razorpay/pay_test_001.json")
                .rawContent("{\"gateway_status\": \"FAILED\", \"auth_status\": \"FAILED\", \"capture_status\": \"NOT_REQUESTED\", \"error\": \"GATEWAY_TIMEOUT_POST_DEBIT\", \"latency_ms\": 65000}")
                .payloadChecksum("1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c")
                .capturedAt(baseTime.plusSeconds(65))
                .build());

        investigationEvidenceRepository.save(InvestigationEvidence.builder()
                .evidenceId("evi_test_003")
                .incidentId(HERO_INCIDENT_ID)
                .paymentId(HERO_PAYMENT_ID)
                .evidenceSource(EvidenceSource.MERCHANT_OMS_LOG)
                .evidenceType(EvidenceType.HTTP_TRACE)
                .filePath("/telemetry/merchant/swiggy/ORD-2026-TEST01.log")
                .rawContent("OMS_ORDER_STATUS: order_id=ORD-2026-TEST01, status=CANCELLED, fulfillment=UNFULFILLED, reason=SESSION_TIMEOUT_NO_PROOF")
                .payloadChecksum("3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d")
                .capturedAt(baseTime.plusSeconds(70))
                .build());

        investigationEvidenceRepository.save(InvestigationEvidence.builder()
                .evidenceId("evi_test_004")
                .incidentId(HERO_INCIDENT_ID)
                .paymentId(HERO_PAYMENT_ID)
                .evidenceSource(EvidenceSource.WEBHOOK_DELIVERY_LOG)
                .evidenceType(EvidenceType.HTTP_TRACE)
                .filePath("/telemetry/webhooks/swiggy/pay_test_001.log")
                .rawContent("WEBHOOK_DISPATCH: target=https://api.swiggy.com/payments/webhook, http_code=504, attempts=3, status=DROPPED")
                .payloadChecksum("5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f")
                .capturedAt(baseTime.plusSeconds(75))
                .build());

        log.info("Hero demo incident {} seeded successfully in clean initial state.", HERO_INCIDENT_ID);
    }

    @Transactional
    public synchronized IncidentCaseDto resetHeroIncident() {
        log.info("Executing authoritative DEMO RESET for incident: {}", HERO_INCIDENT_ID);

        // 1. Delete Resolutions and ML Assessments
        resolutionRepository.findByPaymentId(HERO_PAYMENT_ID).forEach(resolutionRepository::delete);
        resolutionRepository.findByIncidentId(HERO_INCIDENT_ID).ifPresent(resolutionRepository::delete);
        mlAssessmentRepository.findByIncidentId(HERO_INCIDENT_ID).ifPresent(mlAssessmentRepository::delete);

        // 2. Delete Evidence and Events
        investigationEvidenceRepository.findByIncidentId(HERO_INCIDENT_ID).forEach(investigationEvidenceRepository::delete);
        paymentEventRepository.findByPaymentIdOrderByEventTimestampAsc(HERO_PAYMENT_ID).forEach(paymentEventRepository::delete);

        // 3. Delete Incident and Core Records
        incidentCaseRepository.deleteById(HERO_INCIDENT_ID);
        bankRecordRepository.findByPaymentId(HERO_PAYMENT_ID).ifPresent(bankRecordRepository::delete);
        gatewayRecordRepository.findByPaymentId(HERO_PAYMENT_ID).ifPresent(gatewayRecordRepository::delete);
        merchantOrderRecordRepository.findByPaymentId(HERO_PAYMENT_ID).ifPresent(merchantOrderRecordRepository::delete);
        webhookRecordRepository.findByPaymentId(HERO_PAYMENT_ID).ifPresent(webhookRecordRepository::delete);
        settlementRecordRepository.findByPaymentId(HERO_PAYMENT_ID).ifPresent(settlementRecordRepository::delete);
        refundRecordRepository.findByPaymentId(HERO_PAYMENT_ID).ifPresent(refundRecordRepository::delete);
        paymentRepository.findById(HERO_PAYMENT_ID).ifPresent(paymentRepository::delete);

        // 4. Re-seed clean Hero Incident
        seedHeroIncident();

        // 5. Fetch fresh DTO
        IncidentCase incident = incidentCaseRepository.findById(HERO_INCIDENT_ID)
                .orElseThrow(() -> new IllegalStateException("Hero incident failed to re-seed during reset"));

        return IncidentCaseDto.builder()
                .incidentId(incident.getIncidentId())
                .paymentId(incident.getPaymentId())
                .incidentType(incident.getIncidentType())
                .severity(incident.getSeverity())
                .caseStatus(incident.getCaseStatus())
                .triggerSource(incident.getTriggerSource())
                .assignedInvestigator(incident.getAssignedInvestigator())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .openedAt(incident.getOpenedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
