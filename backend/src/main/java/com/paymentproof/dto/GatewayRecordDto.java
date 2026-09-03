package com.paymentproof.dto;

import com.paymentproof.entity.enums.AuthStatus;
import com.paymentproof.entity.enums.CaptureStatus;
import com.paymentproof.entity.enums.GatewayStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayRecordDto {
    private String gatewayRecordId;
    private String paymentId;
    private String gatewayName;
    private String gatewayTransactionId;
    private String gatewayOrderId;
    private AuthStatus authStatus;
    private CaptureStatus captureStatus;
    private GatewayStatus gatewayStatus;
    private BigDecimal authorizedAmount;
    private BigDecimal capturedAmount;
    private BigDecimal fee;
    private BigDecimal tax;
    private String errorCode;
    private String errorDescription;
    private Integer processingLatencyMs;
    private LocalDateTime gatewayTimestamp;
    private String rawPayload;
    private LocalDateTime createdAt;
}
