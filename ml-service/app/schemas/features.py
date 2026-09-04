"""
Pydantic Schemas for Incoming Telemetry Feature Payloads
Supports both Python snake_case and Java camelCase telemetry payloads.
"""

from typing import Optional, Any
from pydantic import BaseModel, Field, model_validator

class PaymentIncidentFeatures(BaseModel):
    payment_id: Optional[str] = Field(None, description="Canonical payment ID (e.g. pay_000001)")
    incident_id: Optional[str] = Field(None, description="Incident ID if triggered from investigation")
    merchant_id: Optional[str] = Field("merch_default", description="Merchant Identifier")
    amount: float = Field(..., gt=0.0, description="Payment transaction amount in INR")
    payment_method: Optional[str] = Field("UPI", description="Payment method: UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET, EMI")
    bank: Optional[str] = Field("HDFC", description="Bank name: HDFC, ICICI, SBI, AXIS, KOTAK, PNB, etc.")
    bank_name: Optional[str] = Field(None, description="Alias for bank name")
    gateway: Optional[str] = Field("RAZORPAY", description="Payment gateway: RAZORPAY, CASHFREE, PAYU, BILLDESK, STRIPE")
    gateway_name: Optional[str] = Field(None, description="Alias for gateway name")
    
    # Telemetry statuses
    bank_status: Optional[str] = Field("SUCCESS", description="Bank status: SUCCESS, DEBITED, FAILED, DECLINED, TIMEOUT, PENDING, REVERSED")
    gateway_status: Optional[str] = Field("SUCCESS", description="Gateway status: SUCCESS, FAILED, TIMED_OUT, PENDING, AUTHORIZED, CANCELLED")
    auth_status: Optional[str] = Field(None, description="Auth status: NONE, AUTHORIZED, FAILED, TIMEOUT")
    gateway_auth_status: Optional[str] = Field(None, description="Alias for auth_status")
    capture_status: Optional[str] = Field(None, description="Capture status: NOT_REQUESTED, PENDING, CAPTURED, FAILED, AUTO_REFUNDED")
    gateway_capture_status: Optional[str] = Field(None, description="Alias for capture_status")
    merchant_order_status: Optional[str] = Field("PAID", description="Merchant OMS status: PAID, CANCELLED, PENDING_PAYMENT, EXPIRED, FAILED, REFUNDED")
    merchant_status: Optional[str] = Field(None, description="Alias for merchant_order_status")
    merchant_fulfillment_status: Optional[str] = Field("DELIVERED", description="Fulfillment: UNFULFILLED, PROCESSING, PACKED, SHIPPED, DELIVERED, CANCELLED, RETURNED")
    merchant_fulfillment: Optional[str] = Field(None, description="Alias for merchant_fulfillment_status")
    webhook_status: Optional[str] = Field("DELIVERED", description="Webhook delivery: DELIVERED, FAILED, DROPPED, TIMED_OUT, RETRYING, SCHEDULED, NONE")
    webhook_http_code: Optional[int] = Field(200, description="Webhook response HTTP code (e.g. 200, 504, 500)")
    webhook_attempt_count: Optional[int] = Field(1, ge=0, description="Number of webhook dispatch attempts")
    webhook_attempts: Optional[int] = Field(None, ge=0, description="Alias for webhook_attempt_count")
    settlement_status: Optional[str] = Field("SETTLED", description="Settlement ledger status: SETTLED, PENDING, ON_HOLD, FAILED, DISCREPANCY, REVERSED, NONE")
    refund_status: Optional[str] = Field("NONE", description="Refund status: PROCESSED, INITIATED, PENDING, FAILED, MANUAL_INTERVENTION_REQUIRED, NONE")
    bank_reversal_status: Optional[str] = Field("NONE", description="Bank clearing reversal status: CREDITED_TO_CUSTOMER, AWAITING_ACK, REJECTED_BY_BANK, NOT_INITIATED, NONE")
    
    # Latencies & Diagnostics
    bank_latency_ms: Optional[int] = Field(350, ge=0, description="Bank switch latency in milliseconds")
    gateway_latency_ms: Optional[int] = Field(500, ge=0, description="Gateway processing latency in milliseconds")
    transaction_age_seconds: Optional[int] = Field(120, ge=0, description="Age of transaction in seconds")
    time_of_day_hour: Optional[int] = Field(12, ge=0, le=23, description="Hour of the day (0-23)")
    retry_count: Optional[int] = Field(0, ge=0, description="Number of user retry attempts")
    amount_deviation_score: Optional[float] = Field(0.0, description="Deviation score from merchant average ticket")
    historical_merchant_failure_rate: Optional[float] = Field(0.04, ge=0.0, le=1.0, description="Merchant historical baseline failure rate")
    is_amount_matched: Optional[Any] = Field(1, description="1 or True if debited amount matches expected amount")
    is_duplicate_candidate: Optional[Any] = Field(0, description="1 or True if duplicate charge candidate detected")

    @model_validator(mode="before")
    @classmethod
    def normalize_aliases(cls, values: Any) -> Any:
        if not isinstance(values, dict):
            return values

        # Convert Java camelCase keys to snake_case
        camel_to_snake = {
            "paymentId": "payment_id",
            "incidentId": "incident_id",
            "merchantId": "merchant_id",
            "paymentMethod": "payment_method",
            "bankName": "bank",
            "bankStatus": "bank_status",
            "bankLatencyMs": "bank_latency_ms",
            "gatewayName": "gateway",
            "gatewayStatus": "gateway_status",
            "gatewayAuthStatus": "auth_status",
            "gatewayCaptureStatus": "capture_status",
            "authStatus": "auth_status",
            "captureStatus": "capture_status",
            "gatewayLatencyMs": "gateway_latency_ms",
            "merchantStatus": "merchant_order_status",
            "merchantOrderStatus": "merchant_order_status",
            "merchantFulfillment": "merchant_fulfillment_status",
            "merchantFulfillmentStatus": "merchant_fulfillment_status",
            "webhookStatus": "webhook_status",
            "webhookHttpCode": "webhook_http_code",
            "webhookAttempts": "webhook_attempt_count",
            "webhookAttemptCount": "webhook_attempt_count",
            "settlementStatus": "settlement_status",
            "refundStatus": "refund_status",
            "bankReversalStatus": "bank_reversal_status",
            "isAmountMatched": "is_amount_matched",
            "isDuplicateCandidate": "is_duplicate_candidate"
        }
        for camel, snake in camel_to_snake.items():
            if camel in values and snake not in values:
                values[snake] = values[camel]

        # Normalize Bank
        if "bank_name" in values and values["bank_name"] is not None and "bank" not in values:
            values["bank"] = values["bank_name"]

        raw_bank = str(values.get("bank", values.get("bank_name", "HDFC"))).upper()
        if "HDFC" in raw_bank:
            values["bank"] = "HDFC"
        elif "ICICI" in raw_bank:
            values["bank"] = "ICICI"
        elif "SBI" in raw_bank:
            values["bank"] = "SBI"
        elif "AXIS" in raw_bank:
            values["bank"] = "AXIS"
        elif "KOTAK" in raw_bank:
            values["bank"] = "KOTAK"
        elif "PNB" in raw_bank:
            values["bank"] = "PNB"
        
        # Normalize Gateway
        if "gateway_name" in values and values["gateway_name"] is not None and "gateway" not in values:
            values["gateway"] = values["gateway_name"]

        # Normalize Auth Status
        if "gateway_auth_status" in values and values["gateway_auth_status"] is not None:
            values["auth_status"] = values["gateway_auth_status"]

        # Normalize Capture Status
        if "gateway_capture_status" in values and values["gateway_capture_status"] is not None:
            values["capture_status"] = values["gateway_capture_status"]

        # Normalize Merchant Status
        if "merchant_status" in values and values["merchant_status"] is not None:
            values["merchant_order_status"] = values["merchant_status"]

        # Normalize Fulfillment Status
        if "merchant_fulfillment" in values and values["merchant_fulfillment"] is not None:
            values["merchant_fulfillment_status"] = values["merchant_fulfillment"]

        # Infer default capture and auth status from gateway status if not explicitly given
        gw_st = str(values.get("gateway_status", "SUCCESS")).upper()
        if "auth_status" not in values or values["auth_status"] is None:
            if gw_st in ["FAILED", "TIMED_OUT", "PENDING"]:
                values["auth_status"] = "TIMEOUT" if gw_st in ["TIMED_OUT", "PENDING"] else "FAILED"
            else:
                values["auth_status"] = "AUTHORIZED"

        if "capture_status" not in values or values["capture_status"] is None:
            if gw_st in ["FAILED", "TIMED_OUT", "PENDING"]:
                values["capture_status"] = "FAILED"
            else:
                values["capture_status"] = "CAPTURED"

        # Normalize Webhook Attempts
        if "webhook_attempts" in values and values["webhook_attempts"] is not None:
            values["webhook_attempt_count"] = values["webhook_attempts"]

        # Normalize boolean flags to int (1 or 0)
        if "is_amount_matched" in values and isinstance(values["is_amount_matched"], bool):
            values["is_amount_matched"] = 1 if values["is_amount_matched"] else 0

        if "is_duplicate_candidate" in values and isinstance(values["is_duplicate_candidate"], bool):
            values["is_duplicate_candidate"] = 1 if values["is_duplicate_candidate"] else 0

        return values
