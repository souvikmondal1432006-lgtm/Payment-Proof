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
        List<PaymentEvent> events = paymentEventRepository.findByPaymentIdOrderByEventTimestampAsc(paymentId);

        // 1. Payment Initialization Event (Only if not already recorded in PaymentEvents)
        boolean hasInitEvent = events.stream().anyMatch(e -> "PAYMENT_INITIATED".equalsIgnoreCase(e.getEventType()));
        if (!hasInitEvent) {
            timeline.add(TimelineEventDto.builder()
                    .eventId("init_" + payment.getPaymentId())
                    .source("CLIENT_SDK")
                    .sourceSystem("CLIENT_SDK")
                    .eventType("PAYMENT_INITIATED")
                    .title("Payment Initiated")
                    .description(String.format("Payment initiated for INR %s via %s (Order: %s)",
                            payment.getAmount(), payment.getPaymentMethod(), payment.getOrderId()))
                    .previousState("Initial state")
                    .newState("INITIATED")
                    .status("INITIATED")
                    .timestamp(payment.getInitiatedAt())
                    .eventTimestamp(payment.getInitiatedAt())
                    .metadata(Map.of("merchantId", payment.getMerchantId(), "clientIp", payment.getClientIp() != null ? payment.getClientIp() : "N/A"))
                    .build());
        }

        // 2. Lifecycle Payment Events
        for (PaymentEvent pe : events) {
            String prev = pe.getFromStatus();
            if (prev == null || prev.trim().isEmpty() || prev.equalsIgnoreCase("NONE")) {
                prev = "Initial state";
            }
            String desc = formatPaymentEventDescription(pe, payment);

            timeline.add(TimelineEventDto.builder()
                    .eventId(pe.getEventId())
                    .source(pe.getEventSource())
                    .sourceSystem(pe.getEventSource())
                    .eventType(pe.getEventType())
                    .title(formatEventTitle(pe.getEventType()))
                    .description(desc)
                    .previousState(prev)
                    .newState(pe.getToStatus())
                    .status(pe.getToStatus())
                    .timestamp(pe.getEventTimestamp())
                    .eventTimestamp(pe.getEventTimestamp())
                    .metadata(pe.getEventPayload() != null ? Map.of("payload", pe.getEventPayload()) : Collections.emptyMap())
                    .build());
        }

        // 3. Bank Switch Telemetry
        bankRecordRepository.findByPaymentId(paymentId).ifPresent(b -> {
            String utr = b.getUtrNumber() != null ? b.getUtrNumber() : "N/A";
            String debited = b.getDebitedAmount() != null ? b.getDebitedAmount().toString() : payment.getAmount().toString();
            String bankNewState = b.getBankStatus() == com.paymentproof.entity.enums.BankStatus.SUCCESS ? "DEBITED (SUCCESS)" : b.getBankStatus().name();

            timeline.add(TimelineEventDto.builder()
                    .eventId("bnk_" + b.getBankRecordId())
                    .source("BANK_SWITCH")
                    .sourceSystem("BANK_SWITCH (" + b.getBankName() + ")")
                    .eventType("BANK_TELEMETRY_RECORDED")
                    .title(String.format("Bank Processing (%s)", b.getBankName()))
                    .description(String.format("Core banking switch reported %s. Account debited INR %s with UTR: %s (Latency: %dms)",
                            b.getBankStatus(), debited, utr, b.getNetworkLatencyMs() != null ? b.getNetworkLatencyMs() : 0))
                    .previousState("INITIATED")
                    .newState(bankNewState)
                    .status(b.getBankStatus().name())
                    .latencyMs(b.getNetworkLatencyMs())
                    .timestamp(b.getBankTimestamp())
                    .eventTimestamp(b.getBankTimestamp())
                    .metadata(Map.of("bankName", b.getBankName(), "debitedAmount", debited, "utr", utr))
                    .build());
        });

        // 4. Gateway Engine Telemetry
        gatewayRecordRepository.findByPaymentId(paymentId).ifPresent(g -> {
            String gwNewState = g.getGatewayStatus() == com.paymentproof.entity.enums.GatewayStatus.FAILED ? "FAILED (TIMEOUT)" : g.getGatewayStatus().name();
            String errDesc = g.getErrorDescription() != null ? g.getErrorDescription() : (g.getErrorCode() != null ? g.getErrorCode() : "Gateway timeout post-debit");

            timeline.add(TimelineEventDto.builder()
                    .eventId("gw_" + g.getGatewayRecordId())
                    .source("GATEWAY_ENGINE")
                    .sourceSystem("GATEWAY_ENGINE (" + g.getGatewayName() + ")")
                    .eventType("GATEWAY_TELEMETRY_RECORDED")
                    .title(String.format("Gateway Engine (%s)", g.getGatewayName()))
                    .description(String.format("Aggregator status: %s (Auth: %s, Capture: %s). %s (Latency: %dms)",
                            g.getGatewayStatus(), g.getAuthStatus(), g.getCaptureStatus(), errDesc,
                            g.getProcessingLatencyMs() != null ? g.getProcessingLatencyMs() : 0))
                    .previousState("PENDING")
                    .newState(gwNewState)
                    .status(g.getGatewayStatus().name())
                    .latencyMs(g.getProcessingLatencyMs())
                    .timestamp(g.getGatewayTimestamp())
                    .eventTimestamp(g.getGatewayTimestamp())
                    .metadata(Map.of("gatewayName", g.getGatewayName(), "errorCode", g.getErrorCode() != null ? g.getErrorCode() : "NONE"))
                    .build());
        });

        // 5. Merchant OMS Order Update
        merchantOrderRecordRepository.findByPaymentId(paymentId).ifPresent(m -> {
            String cancelReason = m.getCancellationReason() != null ? " Reason: " + m.getCancellationReason() : "";

            timeline.add(TimelineEventDto.builder()
                    .eventId("mor_" + m.getMerchantOrderRecordId())
                    .source("MERCHANT_OMS")
                    .sourceSystem("MERCHANT_OMS (" + m.getMerchantId() + ")")
                    .eventType("MERCHANT_ORDER_UPDATED")
                    .title(String.format("Merchant Cart Status (%s)", m.getMerchantId()))
                    .description(String.format("Merchant updated order %s status to %s (Fulfillment: %s)%s",
                            m.getMerchantOrderId(), m.getOrderStatus(), m.getFulfillmentStatus(), cancelReason))
                    .previousState("PENDING")
                    .newState(m.getOrderStatus().name())
                    .status(m.getOrderStatus().name())
                    .timestamp(m.getMerchantUpdatedAt())
                    .eventTimestamp(m.getMerchantUpdatedAt())
                    .metadata(Map.of("merchantOrderId", m.getMerchantOrderId(), "fulfillment", m.getFulfillmentStatus().name()))
                    .build());
        });

        // 6. Webhook Delivery
        webhookRecordRepository.findByPaymentId(paymentId).ifPresent(w -> {
            timeline.add(TimelineEventDto.builder()
                    .eventId("wh_" + w.getWebhookId())
                    .source("WEBHOOK_BROKER")
                    .sourceSystem("WEBHOOK_SERVICE")
                    .eventType("WEBHOOK_NOTIFICATION_ATTEMPT")
                    .title(String.format("Webhook Dispatch (%s)", w.getEventName()))
                    .description(String.format("Webhook delivery to %s resulted in status: %s (HTTP %s) after %d attempt(s)",
                            w.getTargetUrl(), w.getDeliveryStatus(),
                            w.getHttpStatusCode() != null ? w.getHttpStatusCode().toString() : "TIMEOUT",
                            w.getAttemptCount()))
                    .previousState("SCHEDULED")
                    .newState(w.getDeliveryStatus().name())
                    .status(w.getDeliveryStatus().name())
                    .latencyMs(w.getLatencyMs())
                    .timestamp(w.getFirstAttemptAt())
                    .eventTimestamp(w.getFirstAttemptAt())
                    .metadata(Map.of("event", w.getEventName(), "attempts", String.valueOf(w.getAttemptCount())))
                    .build());
        });

        // 7. Settlement Payout
        settlementRecordRepository.findByPaymentId(paymentId).ifPresent(s -> {
            if (s.getSettledAt() != null) {
                timeline.add(TimelineEventDto.builder()
                        .eventId("set_" + s.getSettlementId())
                        .source("SETTLEMENT_LEDGER")
                        .sourceSystem("SETTLEMENT_LEDGER")
                        .eventType("SETTLEMENT_BATCH_POSTED")
                        .title(String.format("Merchant Settlement (%s)", s.getSettlementStatus()))
                        .description(String.format("Settled net INR %s to merchant account %s in batch %s (UTR: %s)",
                                s.getNetSettledAmount(), s.getSettlementBankAccount(), s.getBatchId(),
                                s.getSettlementUtr() != null ? s.getSettlementUtr() : "N/A"))
                        .previousState("PENDING")
                        .newState(s.getSettlementStatus().name())
                        .status(s.getSettlementStatus().name())
                        .timestamp(s.getSettledAt())
                        .eventTimestamp(s.getSettledAt())
                        .metadata(Map.of("batchId", s.getBatchId() != null ? s.getBatchId() : "N/A"))
                        .build());
            }
        });

        // 8. Refund Lifecycle
        refundRecordRepository.findByPaymentId(paymentId).ifPresent(r -> {
            timeline.add(TimelineEventDto.builder()
                    .eventId("ref_" + r.getRefundId())
                    .source("REFUND_SERVICE")
                    .sourceSystem("REFUND_SERVICE")
                    .eventType("REFUND_PROCESSED")
                    .title(String.format("Refund Processing (%s)", r.getRefundStatus()))
                    .description(String.format("Refund of INR %s (%s speed) with ARN: %s. Bank Reversal: %s",
                            r.getAmount(), r.getRefundSpeed(), r.getRefundArn() != null ? r.getRefundArn() : "N/A",
                            r.getBankReversalStatus()))
                    .previousState("INITIATED")
                    .newState(r.getRefundStatus().name())
                    .status(r.getRefundStatus().name())
                    .timestamp(r.getInitiatedAt())
                    .eventTimestamp(r.getInitiatedAt())
                    .metadata(Map.of("reason", r.getRefundReason() != null ? r.getRefundReason() : "Dispute remediation"))
                    .build());
        });

        // Sort all aggregated events in chronological order
        timeline.sort(Comparator.comparing(TimelineEventDto::getTimestamp));

        return timeline;
    }

    private String formatPaymentEventDescription(PaymentEvent pe, Payment payment) {
        String prev = pe.getFromStatus() != null && !pe.getFromStatus().equalsIgnoreCase("NONE") ? pe.getFromStatus() : "Initial state";
        switch (pe.getEventType()) {
            case "PAYMENT_INITIATED":
                return String.format("Customer initiated payment of INR %s via %s (%s)",
                        payment.getAmount(), payment.getPaymentMethod(), payment.getOrderId() != null ? payment.getOrderId() : "Order");
            case "BANK_DEBIT_ACKNOWLEDGED":
                return "Core banking switch acknowledged debit request. Account debited INR " + payment.getAmount() + " (UTR confirmed).";
            case "GATEWAY_TIMEOUT_OCCURRED":
                return "Gateway connection timed out after 65,000ms. No capture confirmation received from upstream PSP.";
            case "MERCHANT_CART_CANCELLED":
                return "Swiggy OMS automatically cancelled order " + (payment.getOrderId() != null ? payment.getOrderId() : "") + " due to missing confirmation within session window.";
            case "WEBHOOK_DELIVERY_FAILED":
                return "Webhook notification delivery failed (HTTP 504 Gateway Timeout) after 3 retries. Notification dropped.";
            default:
                return String.format("Transitioned state from %s to %s via %s", prev, pe.getToStatus(), pe.getEventSource());
        }
    }

    private String formatEventTitle(String eventType) {
        if (eventType == null) return "Event";
        return Arrays.stream(eventType.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(eventType);
    }
}
