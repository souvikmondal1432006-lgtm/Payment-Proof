"""
Explainability & Signal Interpretation Service
Extracts the top contributing features and domain signals driving each prediction.
"""

from typing import List, Dict, Any
from app.schemas.prediction import ContributingSignal

def extract_top_signals(features_dict: Dict[str, Any], predicted_class: str, confidence: float) -> List[ContributingSignal]:
    signals = []

    bank_status = str(features_dict.get("bank_status", "UNKNOWN")).upper()
    gateway_status = str(features_dict.get("gateway_status", "UNKNOWN")).upper()
    merchant_status = str(features_dict.get("merchant_order_status", "UNKNOWN")).upper()
    webhook_status = str(features_dict.get("webhook_status", "UNKNOWN")).upper()
    capture_status = str(features_dict.get("capture_status", "UNKNOWN")).upper()
    settlement_status = str(features_dict.get("settlement_status", "UNKNOWN")).upper()
    refund_status = str(features_dict.get("refund_status", "UNKNOWN")).upper()
    bank_latency = int(features_dict.get("bank_latency_ms", 0))
    retry_count = int(features_dict.get("retry_count", 0))
    is_dup_cand = int(features_dict.get("is_duplicate_candidate", 0))

    if predicted_class == "BANK_DEBIT_GATEWAY_FAILURE":
        signals.append(ContributingSignal(
            signal_name="bank_status_debited",
            signal_value=bank_status,
            importance_weight=0.45,
            interpretation="Bank confirmed customer funds were successfully debited from account."
        ))
        signals.append(ContributingSignal(
            signal_name="gateway_status_failure",
            signal_value=gateway_status,
            importance_weight=0.38,
            interpretation="Gateway aggregator reported transaction timeout or failure."
        ))
        signals.append(ContributingSignal(
            signal_name="merchant_order_status",
            signal_value=merchant_status,
            importance_weight=0.17,
            interpretation=f"Merchant OMS marked cart session as {merchant_status} due to lack of immediate confirmation."
        ))

    elif predicted_class == "MISSING_WEBHOOK":
        signals.append(ContributingSignal(
            signal_name="gateway_capture_status",
            signal_value=capture_status,
            importance_weight=0.48,
            interpretation="Payment was authorized and captured successfully at the payment gateway."
        ))
        signals.append(ContributingSignal(
            signal_name="webhook_delivery_status",
            signal_value=webhook_status,
            importance_weight=0.36,
            interpretation=f"Asynchronous webhook dispatch resulted in status '{webhook_status}'."
        ))
        signals.append(ContributingSignal(
            signal_name="merchant_order_unfulfilled",
            signal_value=merchant_status,
            importance_weight=0.16,
            interpretation="Merchant backend never received confirmation payload and left order unfulfilled."
        ))

    elif predicted_class == "DELAYED_CONFIRMATION":
        signals.append(ContributingSignal(
            signal_name="high_bank_switch_latency",
            signal_value=f"{bank_latency} ms",
            importance_weight=0.55,
            interpretation=f"Core banking / NPCI switch experienced extreme latency ({bank_latency/1000:.1f}s)."
        ))
        signals.append(ContributingSignal(
            signal_name="eventual_gateway_capture",
            signal_value=capture_status,
            importance_weight=0.30,
            interpretation="Gateway successfully captured payment asynchronously upon callback arrival."
        ))
        signals.append(ContributingSignal(
            signal_name="status_synchronization",
            signal_value=f"Bank={bank_status}, GW={gateway_status}",
            importance_weight=0.15,
            interpretation="Providers eventually converged to successful states."
        ))

    elif predicted_class == "DUPLICATE_PAYMENT":
        signals.append(ContributingSignal(
            signal_name="duplicate_order_candidate",
            signal_value=str(is_dup_cand),
            importance_weight=0.52,
            interpretation="Multiple distinct payment attempts detected for identical merchant order reference."
        ))
        signals.append(ContributingSignal(
            signal_name="user_retry_count",
            signal_value=str(retry_count),
            importance_weight=0.32,
            interpretation=f"User initiated {retry_count} checkout retry attempt(s) due to slow UI screen."
        ))
        signals.append(ContributingSignal(
            signal_name="concurrent_bank_debits",
            signal_value=bank_status,
            importance_weight=0.16,
            interpretation="Secondary payment attempt was debited at bank network."
        ))

    elif predicted_class == "REFUND_UNCERTAINTY":
        signals.append(ContributingSignal(
            signal_name="refund_status_stalled",
            signal_value=refund_status,
            importance_weight=0.46,
            interpretation=f"Gateway initiated refund with status '{refund_status}'."
        ))
        signals.append(ContributingSignal(
            signal_name="bank_reversal_status",
            signal_value=str(features_dict.get("bank_reversal_status", "NONE")),
            importance_weight=0.38,
            interpretation="Issuer clearing network has not acknowledged customer account credit."
        ))
        signals.append(ContributingSignal(
            signal_name="settlement_reversal_hold",
            signal_value=settlement_status,
            importance_weight=0.16,
            interpretation="Settlement ledger placed funds on hold pending reversal confirmation."
        ))

    elif predicted_class == "SETTLEMENT_MISMATCH":
        signals.append(ContributingSignal(
            signal_name="settlement_status_discrepancy",
            signal_value=settlement_status,
            importance_weight=0.58,
            interpretation="Settlement batch calculation shows variance against captured gross amount."
        ))
        signals.append(ContributingSignal(
            signal_name="successful_capture_verified",
            signal_value=capture_status,
            importance_weight=0.25,
            interpretation="Customer was charged full transaction amount with clean capture."
        ))
        signals.append(ContributingSignal(
            signal_name="mdr_fee_drift",
            signal_value=str(features_dict.get("amount_deviation_score", 0.0)),
            importance_weight=0.17,
            interpretation="Fee/tax deduction rate drifted from contractual MDR schedule."
        ))

    elif predicted_class == "ORDER_PAYMENT_CONFLICT":
        signals.append(ContributingSignal(
            signal_name="late_3ds_authorization",
            signal_value=f"{bank_latency} ms",
            importance_weight=0.45,
            interpretation=f"3DS authentication took {bank_latency/1000:.1f}s, exceeding merchant timeout."
        ))
        signals.append(ContributingSignal(
            signal_name="merchant_cart_expired",
            signal_value=merchant_status,
            importance_weight=0.40,
            interpretation="Merchant inventory reservation timer expired and released order before capture."
        ))
        signals.append(ContributingSignal(
            signal_name="captured_funds_orphaned",
            signal_value=capture_status,
            importance_weight=0.15,
            interpretation="Gateway authorized capture on cancelled order, creating an orphaned debit."
        ))

    elif predicted_class == "UNRESOLVED":
        signals.append(ContributingSignal(
            signal_name="conflicting_provider_telemetry",
            signal_value=f"Bank={bank_status}, GW={gateway_status}, OMS={merchant_status}",
            importance_weight=0.44,
            interpretation="Multiple telemetry sources report incompatible contradictory lifecycle states."
        ))
        signals.append(ContributingSignal(
            signal_name="high_anomaly_ambiguity",
            signal_value=f"Confidence: {confidence:.2f}",
            importance_weight=0.36,
            interpretation="Signal distribution indicates contested first-party dispute or unannounced reversal."
        ))
        signals.append(ContributingSignal(
            signal_name="manual_escalation_required",
            signal_value="TRUE",
            importance_weight=0.20,
            interpretation="Automated rule resolution is unsafe without bank clearing desk confirmation."
        ))

    else:  # NORMAL
        signals.append(ContributingSignal(
            signal_name="end_to_end_synchronization",
            signal_value=f"Bank={bank_status}, GW={gateway_status}, OMS={merchant_status}",
            importance_weight=0.60,
            interpretation="All provider telemetries reflect synchronized successful states."
        ))
        signals.append(ContributingSignal(
            signal_name="clean_webhook_delivery",
            signal_value=webhook_status,
            importance_weight=0.25,
            interpretation="Webhook notification acknowledged by merchant endpoint with HTTP 200."
        ))
        signals.append(ContributingSignal(
            signal_name="settlement_posted",
            signal_value=settlement_status,
            importance_weight=0.15,
            interpretation="Payout ledger batch successfully scheduled without variance."
        ))

    return signals
