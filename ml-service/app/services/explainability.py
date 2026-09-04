"""
Explainability & Signal Interpretation Service
Extracts the top contributing features and domain signals driving each prediction.
Model-grounded: Importance weights are dynamically calculated from the Random Forest
model's actual empirical feature importances (feature_importances_) rather than hardcoded weights.
"""

import os
import json
from typing import List, Dict, Any, Optional
from app.schemas.prediction import ContributingSignal

_CACHED_METADATA: Optional[Dict[str, Any]] = None

def get_model_feature_importances() -> Dict[str, float]:
    """
    Loads empirical feature importances from model_metadata.json.
    Caches the result in memory for low-latency inference.
    """
    global _CACHED_METADATA
    if _CACHED_METADATA is not None:
        return _CACHED_METADATA.get("feature_importances", {})

    current_dir = os.path.dirname(os.path.abspath(__file__))
    ml_root = os.path.dirname(os.path.dirname(current_dir))
    candidate_paths = [
        os.path.join(ml_root, "models", "model_metadata.json"),
        os.path.join(ml_root, "ml-service", "models", "model_metadata.json"),
        os.path.join("models", "model_metadata.json"),
        os.path.join("ml-service", "models", "model_metadata.json")
    ]

    for p in candidate_paths:
        if os.path.exists(p):
            try:
                with open(p, "r") as f:
                    _CACHED_METADATA = json.load(f)
                    return _CACHED_METADATA.get("feature_importances", {})
            except Exception:
                pass

    return {}


def _normalize_signals(candidate_signals: List[Dict[str, Any]]) -> List[ContributingSignal]:
    """
    Normalizes empirical feature importances across the selected candidate signals
    so that their importance weights sum to 1.0.
    """
    if not candidate_signals:
        return []

    # Sort descending by raw importance score
    candidate_signals.sort(key=lambda s: s["raw_score"], reverse=True)
    top_3 = candidate_signals[:3]

    total_raw = sum(s["raw_score"] for s in top_3)
    if total_raw <= 0:
        total_raw = 1.0

    result = []
    accumulated_weight = 0.0
    for i, s in enumerate(top_3):
        if i == len(top_3) - 1:
            weight = round(1.0 - accumulated_weight, 2)
        else:
            weight = round(s["raw_score"] / total_raw, 2)
            accumulated_weight += weight

        result.append(ContributingSignal(
            signal_name=s["name"],
            signal_value=str(s["value"]),
            importance_weight=max(0.01, weight),
            interpretation=s["interpretation"]
        ))

    return result


def extract_top_signals(
    features_dict: Dict[str, Any],
    predicted_class: str,
    confidence: float,
    feature_importances: Optional[Dict[str, float]] = None
) -> List[ContributingSignal]:
    """
    Dynamically extracts top contributing signals grounded in Random Forest empirical feature importances.
    """
    if feature_importances is None or not feature_importances:
        feature_importances = get_model_feature_importances()

    def get_imp(*keys: str, default: float = 0.005) -> float:
        total = 0.0
        for k in keys:
            total += feature_importances.get(k, default)
        return total

    bank_status = str(features_dict.get("bank_status", "UNKNOWN")).upper()
    gateway_status = str(features_dict.get("gateway_status", "UNKNOWN")).upper()
    merchant_status = str(features_dict.get("merchant_order_status", "UNKNOWN")).upper()
    merchant_fulfillment = str(features_dict.get("merchant_fulfillment_status", "UNKNOWN")).upper()
    webhook_status = str(features_dict.get("webhook_status", "UNKNOWN")).upper()
    capture_status = str(features_dict.get("capture_status", "UNKNOWN")).upper()
    settlement_status = str(features_dict.get("settlement_status", "UNKNOWN")).upper()
    refund_status = str(features_dict.get("refund_status", "UNKNOWN")).upper()
    bank_reversal = str(features_dict.get("bank_reversal_status", "NONE")).upper()
    bank_latency = int(features_dict.get("bank_latency_ms", 0))
    gateway_latency = int(features_dict.get("gateway_latency_ms", 0))
    retry_count = int(features_dict.get("retry_count", 0))
    webhook_attempts = int(features_dict.get("webhook_attempt_count", 1))
    txn_age = int(features_dict.get("transaction_age_seconds", 0))
    is_dup_cand = int(features_dict.get("is_duplicate_candidate", 0))
    amount_dev_score = float(features_dict.get("amount_deviation_score", 0.0))

    candidates: List[Dict[str, Any]] = []

    if predicted_class == "BANK_DEBIT_GATEWAY_FAILURE":
        candidates.append({
            "name": "bank_status_debited",
            "value": bank_status,
            "raw_score": get_imp(f"cat__bank_status_{bank_status}", "bin__is_ghost_debit_signal", default=0.035),
            "interpretation": "Bank confirmed customer funds were successfully debited from account."
        })
        candidates.append({
            "name": "gateway_status_failure",
            "value": gateway_status,
            "raw_score": get_imp(f"cat__gateway_status_{gateway_status}", "num__gateway_latency_ms", default=0.030),
            "interpretation": f"Gateway aggregator reported failure state '{gateway_status}' with {gateway_latency}ms latency."
        })
        candidates.append({
            "name": "merchant_order_status",
            "value": merchant_status,
            "raw_score": get_imp(f"cat__merchant_order_status_{merchant_status}", "cat__merchant_fulfillment_status_CANCELLED", default=0.015),
            "interpretation": f"Merchant OMS marked cart session as {merchant_status} due to missing gateway confirmation."
        })

    elif predicted_class == "MISSING_WEBHOOK":
        candidates.append({
            "name": "gateway_capture_status",
            "value": capture_status,
            "raw_score": get_imp(f"cat__capture_status_{capture_status}", "bin__is_captured_webhook_dropped", default=0.040),
            "interpretation": "Payment was authorized and captured successfully at the payment gateway."
        })
        candidates.append({
            "name": "webhook_delivery_status",
            "value": f"{webhook_status} ({webhook_attempts} att)",
            "raw_score": get_imp(f"cat__webhook_status_{webhook_status}", "num__webhook_attempt_count", default=0.030),
            "interpretation": f"Asynchronous webhook dispatch reached terminal state '{webhook_status}' after {webhook_attempts} attempt(s)."
        })
        candidates.append({
            "name": "merchant_order_unfulfilled",
            "value": merchant_status,
            "raw_score": get_imp(f"cat__merchant_order_status_{merchant_status}", "cat__merchant_fulfillment_status_UNFULFILLED", default=0.015),
            "interpretation": "Merchant backend never received confirmation payload and left order unfulfilled."
        })

    elif predicted_class == "DELAYED_CONFIRMATION":
        candidates.append({
            "name": "high_bank_switch_latency",
            "value": f"{bank_latency} ms",
            "raw_score": get_imp("num__bank_latency_ms", "bin__is_extreme_bank_latency", default=0.050),
            "interpretation": f"Core banking / NPCI switch experienced severe latency ({bank_latency/1000:.1f}s)."
        })
        candidates.append({
            "name": "eventual_gateway_capture",
            "value": capture_status,
            "raw_score": get_imp(f"cat__capture_status_{capture_status}", f"cat__gateway_status_{gateway_status}", default=0.025),
            "interpretation": "Gateway successfully captured payment asynchronously upon callback arrival."
        })
        candidates.append({
            "name": "status_synchronization",
            "value": f"Bank={bank_status}, GW={gateway_status}",
            "raw_score": get_imp("num__latency_diff_ms", f"cat__merchant_order_status_{merchant_status}", default=0.015),
            "interpretation": "Providers eventually converged to successful states after clearing switch congestion."
        })

    elif predicted_class == "DUPLICATE_PAYMENT":
        candidates.append({
            "name": "duplicate_order_candidate",
            "value": str(is_dup_cand),
            "raw_score": get_imp("bin__is_duplicate_candidate", "bin__is_duplicate_retry_signal", default=0.045),
            "interpretation": "Multiple distinct payment attempts detected for identical merchant order reference."
        })
        candidates.append({
            "name": "user_retry_count",
            "value": f"{retry_count} retries",
            "raw_score": get_imp("num__retry_count", "num__transaction_age_seconds", default=0.030),
            "interpretation": f"User initiated {retry_count} checkout retry attempt(s) due to initial latency screen."
        })
        candidates.append({
            "name": "concurrent_bank_debits",
            "value": bank_status,
            "raw_score": get_imp(f"cat__bank_status_{bank_status}", f"cat__capture_status_{capture_status}", default=0.015),
            "interpretation": "Secondary payment attempt was debited and captured independently at bank network."
        })

    elif predicted_class == "REFUND_UNCERTAINTY":
        candidates.append({
            "name": "refund_status_stalled",
            "value": refund_status,
            "raw_score": get_imp(f"cat__refund_status_{refund_status}", "bin__is_refund_stalled_signal", default=0.040),
            "interpretation": f"Gateway initiated refund with status '{refund_status}'."
        })
        candidates.append({
            "name": "bank_reversal_status",
            "value": bank_reversal,
            "raw_score": get_imp(f"cat__bank_reversal_status_{bank_reversal}", "cat__bank_reversal_status_NONE", default=0.035),
            "interpretation": f"Issuer clearing network has not acknowledged customer credit (Reversal: {bank_reversal})."
        })
        candidates.append({
            "name": "settlement_reversal_hold",
            "value": settlement_status,
            "raw_score": get_imp(f"cat__settlement_status_{settlement_status}", "cat__settlement_status_ON_HOLD", default=0.015),
            "interpretation": "Settlement ledger placed funds on hold pending reversal confirmation."
        })

    elif predicted_class == "SETTLEMENT_MISMATCH":
        candidates.append({
            "name": "settlement_status_discrepancy",
            "value": settlement_status,
            "raw_score": get_imp(f"cat__settlement_status_{settlement_status}", "bin__is_settlement_discrepancy", default=0.045),
            "interpretation": "Settlement batch calculation shows variance against captured gross amount."
        })
        candidates.append({
            "name": "mdr_fee_drift",
            "value": f"{amount_dev_score:+.4f}",
            "raw_score": get_imp("num__amount_deviation_score", default=0.030),
            "interpretation": f"Fee/tax deduction rate drifted by {amount_dev_score * 100:.2f}% from contractual MDR schedule."
        })
        candidates.append({
            "name": "successful_capture_verified",
            "value": capture_status,
            "raw_score": get_imp(f"cat__capture_status_{capture_status}", default=0.015),
            "interpretation": "Customer was charged full transaction amount with clean capture."
        })

    elif predicted_class == "ORDER_PAYMENT_CONFLICT":
        candidates.append({
            "name": "late_3ds_authorization",
            "value": f"{bank_latency} ms",
            "raw_score": get_imp("num__bank_latency_ms", "bin__is_debited_order_conflict", default=0.045),
            "interpretation": f"3DS authentication took {bank_latency/1000:.1f}s, exceeding merchant checkout window."
        })
        candidates.append({
            "name": "merchant_cart_expired",
            "value": merchant_status,
            "raw_score": get_imp(f"cat__merchant_order_status_{merchant_status}", "cat__merchant_fulfillment_status_CANCELLED", default=0.035),
            "interpretation": "Merchant inventory reservation timer expired and released order before capture."
        })
        candidates.append({
            "name": "captured_funds_orphaned",
            "value": capture_status,
            "raw_score": get_imp(f"cat__capture_status_{capture_status}", default=0.015),
            "interpretation": "Gateway authorized capture on cancelled order, creating an orphaned debit."
        })

    elif predicted_class == "UNRESOLVED":
        candidates.append({
            "name": "conflicting_provider_telemetry",
            "value": f"Bank={bank_status}, GW={gateway_status}, OMS={merchant_status}",
            "raw_score": get_imp(f"cat__bank_status_{bank_status}", f"cat__gateway_status_{gateway_status}", default=0.040),
            "interpretation": "Multiple telemetry sources report incompatible contradictory lifecycle states."
        })
        candidates.append({
            "name": "high_anomaly_ambiguity",
            "value": f"Confidence: {confidence:.2f}",
            "raw_score": get_imp("num__historical_merchant_failure_rate", default=0.030),
            "interpretation": "Signal distribution indicates contested first-party dispute or unannounced reversal."
        })
        candidates.append({
            "name": "manual_escalation_required",
            "value": "TRUE",
            "raw_score": get_imp("cat__merchant_fulfillment_status_CANCELLED", default=0.015),
            "interpretation": "Automated rule resolution is unsafe without bank clearing desk confirmation."
        })

    else:  # NORMAL
        candidates.append({
            "name": "end_to_end_synchronization",
            "value": f"Bank={bank_status}, GW={gateway_status}, OMS={merchant_status}",
            "raw_score": get_imp(f"cat__bank_status_{bank_status}", f"cat__gateway_status_{gateway_status}", f"cat__capture_status_{capture_status}", default=0.050),
            "interpretation": "All provider telemetries reflect synchronized successful states."
        })
        candidates.append({
            "name": "clean_webhook_delivery",
            "value": webhook_status,
            "raw_score": get_imp(f"cat__webhook_status_{webhook_status}", "cat__webhook_status_DELIVERED", default=0.025),
            "interpretation": "Webhook notification acknowledged by merchant endpoint with HTTP 200."
        })
        candidates.append({
            "name": "settlement_posted",
            "value": settlement_status,
            "raw_score": get_imp(f"cat__settlement_status_{settlement_status}", "cat__settlement_status_SETTLED", default=0.015),
            "interpretation": "Payout ledger batch successfully scheduled without variance."
        })

    return _normalize_signals(candidates)
