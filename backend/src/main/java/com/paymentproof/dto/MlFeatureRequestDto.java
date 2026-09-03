package com.paymentproof.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlFeatureRequestDto {

    @NotBlank(message = "paymentId is required")
    private String paymentId;

    private String incidentId;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;

    private String paymentMethod;
    private String bankName;
    private String bankStatus;
    private Integer bankLatencyMs;
    private String gatewayName;
    private String gatewayStatus;
    private String gatewayAuthStatus;
    private String gatewayCaptureStatus;
    private Integer gatewayLatencyMs;
    private String merchantStatus;
    private String merchantFulfillment;
    private String webhookStatus;
    private Integer webhookHttpCode;
    private Integer webhookAttempts;
    private Boolean isAmountMatched;
}
