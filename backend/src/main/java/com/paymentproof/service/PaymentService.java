package com.paymentproof.service;

import com.paymentproof.dto.*;
import com.paymentproof.entity.Payment;
import com.paymentproof.entity.enums.PaymentStatus;
import com.paymentproof.exception.ResourceNotFoundException;
import com.paymentproof.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BankRecordRepository bankRecordRepository;
    private final GatewayRecordRepository gatewayRecordRepository;
    private final MerchantOrderRecordRepository merchantOrderRecordRepository;
    private final WebhookRecordRepository webhookRecordRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final IncidentCaseRepository incidentCaseRepository;
    private final PaymentEventRepository paymentEventRepository;

    @Transactional(readOnly = true)
    public PagedResponseDto<PaymentDto> getPayments(
            String merchantId,
            PaymentStatus status,
            String paymentMethod,
            String search,
            Pageable pageable) {

        Page<Payment> page = paymentRepository.findWithFilters(
                merchantId,
                status,
                paymentMethod,
                (search != null && !search.isBlank()) ? search.trim() : null,
                pageable
        );

        List<PaymentDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return PagedResponseDto.<PaymentDto>builder()
                .content(dtos)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PaymentDto getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));
        return mapToDto(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDetailDto getPaymentDetail(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentId", paymentId));

        PaymentDto paymentDto = mapToDto(payment);

        BankRecordDto bankRecordDto = bankRecordRepository.findByPaymentId(paymentId)
                .map(b -> BankRecordDto.builder()
                        .bankRecordId(b.getBankRecordId())
                        .paymentId(b.getPaymentId())
                        .bankName(b.getBankName())
                        .bankReferenceNumber(b.getBankReferenceNumber())
                        .utrNumber(b.getUtrNumber())
                        .accountLast4(b.getAccountLast4())
                        .bankStatus(b.getBankStatus())
                        .debitedAmount(b.getDebitedAmount())
                        .currency(b.getCurrency())
                        .responseCode(b.getResponseCode())
                        .responseMessage(b.getResponseMessage())
                        .networkLatencyMs(b.getNetworkLatencyMs())
                        .bankTimestamp(b.getBankTimestamp())
                        .rawPayload(b.getRawPayload())
                        .createdAt(b.getCreatedAt())
                        .build())
                .orElse(null);

        GatewayRecordDto gatewayRecordDto = gatewayRecordRepository.findByPaymentId(paymentId)
                .map(g -> GatewayRecordDto.builder()
                        .gatewayRecordId(g.getGatewayRecordId())
                        .paymentId(g.getPaymentId())
                        .gatewayName(g.getGatewayName())
                        .gatewayTransactionId(g.getGatewayTransactionId())
                        .gatewayOrderId(g.getGatewayOrderId())
                        .authStatus(g.getAuthStatus())
                        .captureStatus(g.getCaptureStatus())
                        .gatewayStatus(g.getGatewayStatus())
                        .authorizedAmount(g.getAuthorizedAmount())
                        .capturedAmount(g.getCapturedAmount())
                        .fee(g.getFee())
                        .tax(g.getTax())
                        .errorCode(g.getErrorCode())
                        .errorDescription(g.getErrorDescription())
                        .processingLatencyMs(g.getProcessingLatencyMs())
                        .gatewayTimestamp(g.getGatewayTimestamp())
                        .rawPayload(g.getRawPayload())
                        .createdAt(g.getCreatedAt())
                        .build())
                .orElse(null);

        MerchantOrderRecordDto merchantOrderDto = merchantOrderRecordRepository.findByPaymentId(paymentId)
                .map(m -> MerchantOrderRecordDto.builder()
                        .merchantOrderRecordId(m.getMerchantOrderRecordId())
                        .paymentId(m.getPaymentId())
                        .merchantId(m.getMerchantId())
                        .merchantOrderId(m.getMerchantOrderId())
                        .orderStatus(m.getOrderStatus())
                        .fulfillmentStatus(m.getFulfillmentStatus())
                        .expectedAmount(m.getExpectedAmount())
                        .currency(m.getCurrency())
                        .cancellationReason(m.getCancellationReason())
                        .customerNotes(m.getCustomerNotes())
                        .merchantUpdatedAt(m.getMerchantUpdatedAt())
                        .createdAt(m.getCreatedAt())
                        .build())
                .orElse(null);

        WebhookRecordDto webhookDto = webhookRecordRepository.findByPaymentId(paymentId)
                .map(w -> WebhookRecordDto.builder()
                        .webhookId(w.getWebhookId())
                        .paymentId(w.getPaymentId())
                        .merchantId(w.getMerchantId())
                        .eventName(w.getEventName())
                        .targetUrl(w.getTargetUrl())
                        .attemptCount(w.getAttemptCount())
                        .maxAttempts(w.getMaxAttempts())
                        .deliveryStatus(w.getDeliveryStatus())
                        .httpStatusCode(w.getHttpStatusCode())
                        .latencyMs(w.getLatencyMs())
                        .requestPayloadHash(w.getRequestPayloadHash())
                        .requestPayload(w.getRequestPayload())
                        .responseBody(w.getResponseBody())
                        .firstAttemptAt(w.getFirstAttemptAt())
                        .lastAttemptAt(w.getLastAttemptAt())
                        .nextRetryAt(w.getNextRetryAt())
                        .createdAt(w.getCreatedAt())
                        .build())
                .orElse(null);

        SettlementRecordDto settlementDto = settlementRecordRepository.findByPaymentId(paymentId)
                .map(s -> SettlementRecordDto.builder()
                        .settlementId(s.getSettlementId())
                        .paymentId(s.getPaymentId())
                        .merchantId(s.getMerchantId())
                        .batchId(s.getBatchId())
                        .grossAmount(s.getGrossAmount())
                        .feeDeducted(s.getFeeDeducted())
                        .taxDeducted(s.getTaxDeducted())
                        .netSettledAmount(s.getNetSettledAmount())
                        .currency(s.getCurrency())
                        .settlementStatus(s.getSettlementStatus())
                        .settlementUtr(s.getSettlementUtr())
                        .settlementBankAccount(s.getSettlementBankAccount())
                        .settledAt(s.getSettledAt())
                        .createdAt(s.getCreatedAt())
                        .updatedAt(s.getUpdatedAt())
                        .build())
                .orElse(null);

        RefundRecordDto refundDto = refundRecordRepository.findByPaymentId(paymentId)
                .map(r -> RefundRecordDto.builder()
                        .refundId(r.getRefundId())
                        .paymentId(r.getPaymentId())
                        .merchantId(r.getMerchantId())
                        .gatewayRefundId(r.getGatewayRefundId())
                        .refundArn(r.getRefundArn())
                        .amount(r.getAmount())
                        .currency(r.getCurrency())
                        .refundReason(r.getRefundReason())
                        .refundSpeed(r.getRefundSpeed())
                        .refundStatus(r.getRefundStatus())
                        .bankReversalStatus(r.getBankReversalStatus())
                        .initiatedAt(r.getInitiatedAt())
                        .processedAt(r.getProcessedAt())
                        .rawResponse(r.getRawResponse())
                        .build())
                .orElse(null);

        IncidentCaseDto incidentDto = incidentCaseRepository.findFirstByPaymentIdOrderByOpenedAtDesc(paymentId)
                .map(i -> IncidentCaseDto.builder()
                        .incidentId(i.getIncidentId())
                        .paymentId(i.getPaymentId())
                        .incidentType(i.getIncidentType())
                        .severity(i.getSeverity())
                        .caseStatus(i.getCaseStatus())
                        .triggerSource(i.getTriggerSource())
                        .assignedInvestigator(i.getAssignedInvestigator())
                        .title(i.getTitle())
                        .description(i.getDescription())
                        .openedAt(i.getOpenedAt())
                        .resolvedAt(i.getResolvedAt())
                        .updatedAt(i.getUpdatedAt())
                        .build())
                .orElse(null);

        List<PaymentEventDto> recentEvents = paymentEventRepository.findByPaymentIdOrderByEventTimestampAsc(paymentId).stream()
                .map(e -> PaymentEventDto.builder()
                        .eventId(e.getEventId())
                        .paymentId(e.getPaymentId())
                        .eventType(e.getEventType())
                        .fromStatus(e.getFromStatus())
                        .toStatus(e.getToStatus())
                        .eventSource(e.getEventSource())
                        .eventPayload(e.getEventPayload())
                        .eventTimestamp(e.getEventTimestamp())
                        .build())
                .collect(Collectors.toList());

        return PaymentDetailDto.builder()
                .payment(paymentDto)
                .bankRecord(bankRecordDto)
                .gatewayRecord(gatewayRecordDto)
                .merchantOrderRecord(merchantOrderDto)
                .webhookRecord(webhookDto)
                .settlementRecord(settlementDto)
                .refundRecord(refundDto)
                .activeIncident(incidentDto)
                .recentEvents(recentEvents)
                .build();
    }

    public PaymentDto mapToDto(Payment p) {
        if (p == null) return null;
        return PaymentDto.builder()
                .paymentId(p.getPaymentId())
                .merchantId(p.getMerchantId())
                .customerId(p.getCustomerId())
                .orderId(p.getOrderId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentMethod(p.getPaymentMethod())
                .paymentMethodSubtype(p.getPaymentMethodSubtype())
                .status(p.getStatus())
                .clientIp(p.getClientIp())
                .userAgent(p.getUserAgent())
                .initiatedAt(p.getInitiatedAt())
                .completedAt(p.getCompletedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
