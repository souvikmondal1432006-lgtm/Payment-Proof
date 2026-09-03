package com.paymentproof.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDetailDto {
    private PaymentDto payment;
    private BankRecordDto bankRecord;
    private GatewayRecordDto gatewayRecord;
    private MerchantOrderRecordDto merchantOrderRecord;
    private WebhookRecordDto webhookRecord;
    private SettlementRecordDto settlementRecord;
    private RefundRecordDto refundRecord;
    private IncidentCaseDto activeIncident;
    private List<PaymentEventDto> recentEvents;
}
