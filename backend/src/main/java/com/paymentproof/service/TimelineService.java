package com.paymentproof.service;

import com.paymentproof.dto.TimelineEventDto;
import com.paymentproof.entity.*;
import com.paymentproof.exception.ResourceNotFoundException;
import com.paymentproof.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final BankRecordRepository bankRecordRepository;
    private final GatewayRecordRepository gatewayRecordRepository;
    private final MerchantOrderRecordRepository merchantOrderRecordRepository;
    private final WebhookRecordRepository webhookRecordRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final AuditEventRepository auditEventRepository;

    @Transactional(readOnly = true)
    public List<TimelineEventDto> getTimelineForPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));

        List<TimelineEventDto> timeline = new ArrayList<>();

        // 1. Payment Initialization Event
        timeline.add(TimelineEventDto.builder()
                .eventId("init_" + payment.getPaymentId())
                .source("CLIENT_SDK")
                .eventType("PAYMENT_INITIATED")
                .title("Payment Session Created")
                .description(String.format("Payment initialized for INR %s via %s (Order: %s)",
                        payment.getAmount(), payment.getPaymentMethod(), payment.getOrderId()))
                .status(payment.getStatus().name())
                .timestamp(payment.getInitiatedAt())
                .metadata(Map.of("merchantId", payment.getMerchantId(), "clientIp", payment.getClientIp() != null ? payment.getClientIp() : "N/A"))
                .build());

        // 2. Lifecycle Payment Events
        List<PaymentEvent> events = paymentEventRepository.findByPaymentIdOrderByEventTimestampAsc(paymentId);
        for (PaymentEvent pe : events) {
            timeline.add(TimelineEventDto.builder()
                    .eventId(pe.getEventId())
                    .source(pe.getEventSource())
                    .eventType(pe.getEventType())
                    .title(formatEventTitle(pe.getEventType()))
                    .description(String.format("Transitioned state from %s to %s via %s",
                            pe.getFromStatus() != null ? pe.getFromStatus() : "NONE", pe.getToStatus(), pe.getEventSource()))
                    .status(pe.getToStatus())
                    .timestamp(pe.getEventTimestamp())
                    .metadata(pe.getEventPayload() != null ? Map.of("payload", pe.getEventPayload()) : Collections.emptyMap())
                    .build());
        }

        // 3. Bank Switch Telemetry
        bankRecordRepository.findByPaymentId(paymentId).ifPresent(b -> {
            timeline.add(TimelineEventDto.builder()
                    .eventId("bnk_" + b.getBankRecordId())
                    .source("BANK_SWITCH")
                    .eventType("BANK_TELEMETRY_RECORDED")
                    .title(String.format("Bank Processing (%s)", b.getBankName()))
                    .description(String.format("Core banking switch reported status: %s with UTR: %s (Code: %s - %s)",
                            b.getBankStatus(), b.getUtrNumber() != null ? b.getUtrNumber() : "N/A",
                            b.getResponseCode() != null ? b.getResponseCode() : "N/A",
                            b.getResponseMessage() != null ? b.getResponseMessage() : ""))
                    .status(b.getBankStatus().name())
                    .latencyMs(b.getNetworkLatencyMs())
                    .timestamp(b.getBankTimestamp())
                    .metadata(Map.of("bankName", b.getBankName(), "debitedAmount", b.getDebitedAmount() != null ? b.getDebitedAmount().toString() : "0.00"))
                    .build());
        });

        // 4. Gateway Engine Telemetry
        gatewayRecordRepository.findByPaymentId(paymentId).ifPresent(g -> {
            timeline.add(TimelineEventDto.builder()
                    .eventId("gw_" + g.getGatewayRecordId())
                    .source("GATEWAY_ENGINE")
                    .eventType("GATEWAY_TELEMETRY_RECORDED")
                    .title(String.format("Gateway Engine (%s)", g.getGatewayName()))
                    .description(String.format("Aggregator status: %s (Auth: %s, Capture: %s). Fee: INR %s",
                            g.getGatewayStatus(), g.getAuthStatus(), g.getCaptureStatus(), g.getFee()))
                    .status(g.getGatewayStatus().name())
                    .latencyMs(g.getProcessingLatencyMs())
                    .timestamp(g.getGatewayTimestamp())
                    .metadata(Map.of("gatewayName", g.getGatewayName(), "errorCode", g.getErrorCode() != null ? g.getErrorCode() : "NONE"))
                    .build());
        });

        // 5. Merchant OMS Order Update
        merchantOrderRecordRepository.findByPaymentId(paymentId).ifPresent(m -> {
            timeline.add(TimelineEventDto.builder()
                    .eventId("mor_" + m.getMerchantOrderRecordId())
                    .source("MERCHANT_OMS")
                    .eventType("MERCHANT_ORDER_UPDATED")
                    .title(String.format("Merchant Cart Status (%s)", m.getMerchantId()))
                    .description(String.format("Merchant updated order %s status to %s (Fulfillment: %s)%s",
                            m.getMerchantOrderId(), m.getOrderStatus(), m.getFulfillmentStatus(),
                            m.getCancellationReason() != null ? " Reason: " + m.getCancellationReason() : ""))
                    .status(m.getOrderStatus().name())
                    .timestamp(m.getMerchantUpdatedAt())
                    .metadata(Map.of("merchantOrderId", m.getMerchantOrderId(), "fulfillment", m.getFulfillmentStatus().name()))
                    .build());
        });

        // 6. Webhook Delivery
        webhookRecordRepository.findByPaymentId(paymentId).ifPresent(w -> {
            timeline.add(TimelineEventDto.builder()
                    .eventId("wh_" + w.getWebhookId())
                    .source("WEBHOOK_BROKER")
                    .eventType("WEBHOOK_NOTIFICATION_ATTEMPT")
                    .title(String.format("Webhook Dispatch (%s)", w.getEventName()))
                    .description(String.format("Webhook delivery to %s resulted in status: %s (HTTP %s) after %d attempt(s)",
                            w.getTargetUrl(), w.getDeliveryStatus(),
                            w.getHttpStatusCode() != null ? w.getHttpStatusCode().toString() : "TIMEOUT",
                            w.getAttemptCount()))
                    .status(w.getDeliveryStatus().name())
                    .latencyMs(w.getLatencyMs())
                    .timestamp(w.getFirstAttemptAt())
                    .metadata(Map.of("event", w.getEventName(), "attempts", String.valueOf(w.getAttemptCount())))
                    .build());
        });

        // 7. Settlement Payout
        settlementRecordRepository.findByPaymentId(paymentId).ifPresent(s -> {
            if (s.getSettledAt() != null) {
                timeline.add(TimelineEventDto.builder()
                        .eventId("set_" + s.getSettlementId())
                        .source("SETTLEMENT_LEDGER")
                        .eventType("SETTLEMENT_BATCH_POSTED")
                        .title(String.format("Merchant Settlement (%s)", s.getSettlementStatus()))
                        .description(String.format("Settled net INR %s to merchant account %s in batch %s (UTR: %s)",
                                s.getNetSettledAmount(), s.getSettlementBankAccount(), s.getBatchId(),
                                s.getSettlementUtr() != null ? s.getSettlementUtr() : "N/A"))
                        .status(s.getSettlementStatus().name())
                        .timestamp(s.getSettledAt())
                        .metadata(Map.of("batchId", s.getBatchId() != null ? s.getBatchId() : "N/A"))
                        .build());
            }
        });

        // 8. Refund Lifecycle
        refundRecordRepository.findByPaymentId(paymentId).ifPresent(r -> {
            timeline.add(TimelineEventDto.builder()
                    .eventId("ref_" + r.getRefundId())
                    .source("REFUND_SERVICE")
                    .eventType("REFUND_PROCESSED")
                    .title(String.format("Refund Processing (%s)", r.getRefundStatus()))
                    .description(String.format("Refund of INR %s (%s speed) with ARN: %s. Bank Reversal: %s",
                            r.getAmount(), r.getRefundSpeed(), r.getRefundArn() != null ? r.getRefundArn() : "N/A",
                            r.getBankReversalStatus()))
                    .status(r.getRefundStatus().name())
                    .timestamp(r.getInitiatedAt())
                    .metadata(Map.of("reason", r.getRefundReason()))
                    .build());
        });

        // Sort all aggregated events in chronological order
        timeline.sort(Comparator.comparing(TimelineEventDto::getTimestamp));

        return timeline;
    }

    private String formatEventTitle(String eventType) {
        if (eventType == null) return "Event";
        return Arrays.stream(eventType.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(eventType);
    }
}
