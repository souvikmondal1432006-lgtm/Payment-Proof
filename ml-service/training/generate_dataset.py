"""
Synthetic Historical Payment Incident Dataset Generator
Generates realistic payment records across 9 distinct incident classes
with realistic telemetry noise, overlapping edge cases, latencies, and status distributions.
Strictly adheres to: NO target label leakage into observable feature fields.
"""

import os
import random
import numpy as np
import pandas as pd

# Set fixed random seeds for strict reproducibility
SEED = 42
random.seed(SEED)
np.random.seed(SEED)

CLASSES = [
    "NORMAL",
    "DELAYED_CONFIRMATION",
    "BANK_DEBIT_GATEWAY_FAILURE",
    "MISSING_WEBHOOK",
    "DUPLICATE_PAYMENT",
    "REFUND_UNCERTAINTY",
    "SETTLEMENT_MISMATCH",
    "ORDER_PAYMENT_CONFLICT",
    "UNRESOLVED"
]

BANKS = ["HDFC", "ICICI", "SBI", "AXIS", "KOTAK", "PNB", "YES_BANK", "INDUSIND"]
GATEWAYS = ["RAZORPAY", "CASHFREE", "PAYU", "BILLDESK", "STRIPE"]
PAYMENT_METHODS = ["UPI", "CREDIT_CARD", "DEBIT_CARD", "NET_BANKING", "WALLET", "EMI"]
MERCHANTS = [
    {"id": "merch_flipkart", "avg_ticket": 3500.0, "failure_rate": 0.04},
    {"id": "merch_swiggy", "avg_ticket": 650.0, "failure_rate": 0.03},
    {"id": "merch_zomato", "avg_ticket": 720.0, "failure_rate": 0.03},
    {"id": "merch_amazon_in", "avg_ticket": 2800.0, "failure_rate": 0.02},
    {"id": "merch_myntra", "avg_ticket": 1900.0, "failure_rate": 0.05},
    {"id": "merch_zepto", "avg_ticket": 480.0, "failure_rate": 0.04},
    {"id": "merch_blinkit", "avg_ticket": 420.0, "failure_rate": 0.04},
    {"id": "merch_tatacliq", "avg_ticket": 4800.0, "failure_rate": 0.06},
    {"id": "merch_bookmyshow", "avg_ticket": 1100.0, "failure_rate": 0.05},
    {"id": "merch_makemytrip", "avg_ticket": 12500.0, "failure_rate": 0.07}
]

def generate_sample(incident_class: str, sample_idx: int) -> dict:
    merchant = random.choice(MERCHANTS)
    merchant_id = merchant["id"]
    base_ticket = merchant["avg_ticket"]
    hist_fail_rate = merchant["failure_rate"] + random.uniform(-0.01, 0.01)

    # Base financial amount with realistic log-normal variation
    raw_amount = max(49.0, np.random.lognormal(mean=np.log(base_ticket), sigma=0.6))
    amount = round(float(raw_amount), 2)
    amount_deviation = round((amount - base_ticket) / base_ticket, 4)

    payment_method = random.choices(
        PAYMENT_METHODS, weights=[0.55, 0.20, 0.10, 0.08, 0.04, 0.03]
    )[0]
    bank = random.choice(BANKS)
    gateway = random.choice(GATEWAYS)
    time_of_day_hour = random.randint(0, 23)
    transaction_age_seconds = random.randint(30, 86400 * 3)

    # Default baseline statuses
    bank_status = "SUCCESS"
    gateway_status = "SUCCESS"
    auth_status = "AUTHORIZED"
    capture_status = "CAPTURED"
    merchant_order_status = "PAID"
    merchant_fulfillment_status = "DELIVERED" if transaction_age_seconds > 86400 else "PROCESSING"
    webhook_status = "DELIVERED"
    webhook_http_code = 200
    webhook_attempt_count = 1
    settlement_status = "SETTLED" if transaction_age_seconds > 86400 else "PENDING"
    refund_status = "NONE"
    bank_reversal_status = "NONE"
    bank_latency_ms = random.randint(180, 750)
    gateway_latency_ms = bank_latency_ms + random.randint(80, 300)
    retry_count = 0
    is_amount_matched = True
    is_duplicate_candidate = False

    # Introduce realistic edge case telemetry & cross-system noise
    if incident_class == "NORMAL":
        # 15% of normal payments experience benign transient delays, webhook retries, or user double-clicks
        if random.random() < 0.15:
            bank_latency_ms = random.randint(1500, 8000)
            gateway_latency_ms = bank_latency_ms + random.randint(400, 1500)
        if random.random() < 0.08:
            webhook_attempt_count = random.choice([2, 3])
            webhook_status = "DELIVERED"
        if random.random() < 0.06:
            retry_count = 1  # Benign client-side double click or reconnect
            is_duplicate_candidate = (random.random() < 0.50)  # SDK flagged candidate, but only 1 debit occurred
        if random.random() < 0.03:
            amount_deviation = round(random.uniform(0.005, 0.02), 4)

    elif incident_class == "DELAYED_CONFIRMATION":
        # Bank switch latency elevated (20s to 90s); eventual capture with occasional user retries
        bank_status = random.choices(["SUCCESS", "PENDING", "DEBITED"], weights=[0.80, 0.15, 0.05])[0]
        bank_latency_ms = random.randint(20000, 90000)
        gateway_status = random.choices(["SUCCESS", "PENDING"], weights=[0.85, 0.15])[0]
        auth_status = "AUTHORIZED"
        capture_status = random.choices(["CAPTURED", "PENDING"], weights=[0.85, 0.15])[0]
        gateway_latency_ms = bank_latency_ms + random.randint(800, 5000)
        merchant_order_status = random.choices(["PAID", "PROCESSING", "PENDING_PAYMENT"], weights=[0.60, 0.25, 0.15])[0]
        merchant_fulfillment_status = "PROCESSING"
        webhook_status = random.choices(["DELIVERED", "RETRYING", "SCHEDULED"], weights=[0.75, 0.20, 0.05])[0]
        webhook_attempt_count = random.choice([1, 2, 3])
        if random.random() < 0.12:
            retry_count = 1  # User retried while waiting
            is_duplicate_candidate = True

    elif incident_class == "BANK_DEBIT_GATEWAY_FAILURE":
        # Bank debited, gateway timed out (30s to 75s) or failed, merchant cancelled cart
        bank_status = random.choices(["SUCCESS", "DEBITED"], weights=[0.65, 0.35])[0]
        bank_latency_ms = random.randint(250, 2500)
        gateway_status = random.choices(["FAILED", "TIMED_OUT", "PENDING"], weights=[0.65, 0.25, 0.10])[0]
        auth_status = random.choices(["TIMEOUT", "FAILED", "NONE"], weights=[0.50, 0.35, 0.15])[0]
        capture_status = random.choices(["FAILED", "NOT_REQUESTED", "AUTO_REFUNDED"], weights=[0.70, 0.18, 0.12])[0]
        gateway_latency_ms = random.randint(30000, 75000)
        merchant_order_status = random.choices(["CANCELLED", "EXPIRED", "FAILED"], weights=[0.75, 0.20, 0.05])[0]
        merchant_fulfillment_status = "CANCELLED"
        webhook_status = random.choices(["DELIVERED", "FAILED", "NONE"], weights=[0.35, 0.40, 0.25])[0]
        webhook_http_code = 200 if webhook_status == "DELIVERED" else random.choice([504, 500, 0])
        settlement_status = "ON_HOLD"
        refund_status = random.choices(["INITIATED", "PROCESSED", "NONE"], weights=[0.35, 0.25, 0.40])[0]
        bank_reversal_status = "CREDITED_TO_CUSTOMER" if refund_status == "PROCESSED" else "NOT_INITIATED"

    elif incident_class == "MISSING_WEBHOOK":
        # Gateway captured payment, but webhook delivery dropped/failed
        bank_status = "SUCCESS"
        gateway_status = "SUCCESS"
        auth_status = "AUTHORIZED"
        capture_status = "CAPTURED"
        merchant_order_status = random.choices(["PENDING_PAYMENT", "EXPIRED", "CANCELLED", "PAID"], weights=[0.55, 0.25, 0.15, 0.05])[0]
        merchant_fulfillment_status = "UNFULFILLED"
        webhook_status = random.choices(["DROPPED", "FAILED", "TIMED_OUT"], weights=[0.50, 0.35, 0.15])[0]
        webhook_http_code = random.choice([504, 500, 502, 404, 0])
        webhook_attempt_count = random.choice([2, 3, 4, 5])
        settlement_status = random.choices(["ON_HOLD", "PENDING", "SETTLED"], weights=[0.60, 0.30, 0.10])[0]

    elif incident_class == "DUPLICATE_PAYMENT":
        # Customer retried after slow screen; 2 distinct debits
        bank_status = "SUCCESS"
        gateway_status = "SUCCESS"
        auth_status = "AUTHORIZED"
        capture_status = "CAPTURED"
        merchant_order_status = "PAID"
        merchant_fulfillment_status = "DELIVERED"
        retry_count = random.randint(1, 4)
        is_duplicate_candidate = (random.random() < 0.85)  # 85% candidate detected, 15% stealth duplicate
        settlement_status = random.choices(["ON_HOLD", "DISCREPANCY", "SETTLED"], weights=[0.60, 0.25, 0.15])[0]
        refund_status = random.choices(["INITIATED", "PROCESSED", "NONE"], weights=[0.40, 0.30, 0.30])[0]
        bank_reversal_status = "CREDITED_TO_CUSTOMER" if refund_status == "PROCESSED" else "AWAITING_ACK"

    elif incident_class == "REFUND_UNCERTAINTY":
        # Refund initiated at gateway, but bank clearing reversal is stalled/unacknowledged
        bank_status = random.choices(["SUCCESS", "REVERSED"], weights=[0.80, 0.20])[0]
        gateway_status = "SUCCESS"
        merchant_order_status = "REFUNDED"
        merchant_fulfillment_status = "RETURNED"
        refund_status = random.choices(["MANUAL_INTERVENTION_REQUIRED", "PENDING", "FAILED"], weights=[0.50, 0.35, 0.15])[0]
        bank_reversal_status = random.choices(["AWAITING_ACK", "REJECTED_BY_BANK", "NOT_INITIATED"], weights=[0.55, 0.30, 0.15])[0]
        settlement_status = random.choices(["REVERSED", "ON_HOLD"], weights=[0.85, 0.15])[0]

    elif incident_class == "SETTLEMENT_MISMATCH":
        # Capture succeeded, but payout batch has discrepancy in fee, tax, or net settled amount
        bank_status = "SUCCESS"
        gateway_status = "SUCCESS"
        auth_status = "AUTHORIZED"
        capture_status = "CAPTURED"
        merchant_order_status = "PAID"
        merchant_fulfillment_status = "DELIVERED"
        # 65% flagged as DISCREPANCY, 35% flagged as SETTLED/ON_HOLD with non-zero fee drift
        settlement_status = random.choices(["DISCREPANCY", "SETTLED", "ON_HOLD"], weights=[0.65, 0.25, 0.10])[0]
        amount_deviation = round(random.uniform(0.025, 0.095), 4)

    elif incident_class == "ORDER_PAYMENT_CONFLICT":
        # Merchant timed out inventory reservation at 2-5m, but 3DS finished late
        bank_status = "SUCCESS"
        bank_latency_ms = random.randint(115000, 310000)  # ~2 to 5.2 minutes
        gateway_status = "SUCCESS"
        auth_status = "AUTHORIZED"
        capture_status = "CAPTURED"
        gateway_latency_ms = bank_latency_ms + random.randint(500, 3500)
        merchant_order_status = random.choices(["CANCELLED", "EXPIRED"], weights=[0.80, 0.20])[0]
        merchant_fulfillment_status = "CANCELLED"
        webhook_status = random.choices(["DELIVERED", "FAILED"], weights=[0.85, 0.15])[0]
        settlement_status = "ON_HOLD"
        refund_status = random.choices(["INITIATED", "PROCESSED", "NONE"], weights=[0.40, 0.30, 0.30])[0]

    elif incident_class == "UNRESOLVED":
        # High ambiguity: Complex chargeback, conflicting multi-party statuses, borderline latencies
        bank_status = random.choice(["REVERSED", "PENDING", "DEBITED", "SUCCESS"])
        gateway_status = random.choice(["SUCCESS", "PENDING", "FAILED", "TIMED_OUT"])
        auth_status = random.choice(["AUTHORIZED", "TIMEOUT", "NONE", "FAILED"])
        capture_status = random.choice(["CAPTURED", "PENDING", "FAILED", "AUTO_REFUNDED"])
        merchant_order_status = random.choice(["PAID", "CANCELLED", "PENDING_PAYMENT", "FAILED", "EXPIRED"])
        merchant_fulfillment_status = random.choice(["DELIVERED", "UNFULFILLED", "CANCELLED", "RETURNED", "PROCESSING"])
        webhook_status = random.choice(["DELIVERED", "FAILED", "DROPPED", "RETRYING", "SCHEDULED"])
        webhook_http_code = random.choice([200, 500, 504, 404, 0, 502])
        settlement_status = random.choice(["ON_HOLD", "DISCREPANCY", "FAILED", "PENDING"])
        refund_status = random.choice(["PENDING", "MANUAL_INTERVENTION_REQUIRED", "NONE", "FAILED"])
        bank_reversal_status = random.choice(["AWAITING_ACK", "REJECTED_BY_BANK", "NOT_INITIATED", "CREDITED_TO_CUSTOMER"])
        retry_count = random.randint(0, 3)
        bank_latency_ms = random.randint(350, 140000)
        gateway_latency_ms = random.randint(450, 150000)

    # Add realistic continuous latency noise and telemetry jitter (6% random jitter)
    if random.random() < 0.06:
        bank_latency_ms = max(80, int(bank_latency_ms * random.uniform(0.85, 1.25)))
        gateway_latency_ms = max(bank_latency_ms, int(gateway_latency_ms * random.uniform(0.90, 1.20)))

    return {
        "payment_id": f"pay_synth_{sample_idx:07d}",
        "merchant_id": merchant_id,
        "amount": amount,
        "payment_method": payment_method,
        "bank": bank,
        "gateway": gateway,
        "bank_status": bank_status,
        "gateway_status": gateway_status,
        "auth_status": auth_status,
        "capture_status": capture_status,
        "merchant_order_status": merchant_order_status,
        "merchant_fulfillment_status": merchant_fulfillment_status,
        "webhook_status": webhook_status,
        "webhook_http_code": webhook_http_code if webhook_http_code is not None else 0,
        "webhook_attempt_count": webhook_attempt_count,
        "settlement_status": settlement_status,
        "refund_status": refund_status,
        "bank_reversal_status": bank_reversal_status,
        "bank_latency_ms": bank_latency_ms,
        "gateway_latency_ms": gateway_latency_ms,
        "transaction_age_seconds": transaction_age_seconds,
        "time_of_day_hour": time_of_day_hour,
        "retry_count": retry_count,
        "amount_deviation_score": amount_deviation,
        "historical_merchant_failure_rate": round(hist_fail_rate, 4),
        "is_amount_matched": 1 if is_amount_matched else 0,
        "is_duplicate_candidate": 1 if is_duplicate_candidate else 0,
        "target_incident_class": incident_class
    }

def generate_full_dataset(total_samples: int = 7000, output_dir: str = "data") -> pd.DataFrame:
    os.makedirs(output_dir, exist_ok=True)
    
    class_distribution = {
        "NORMAL": int(total_samples * 0.44),
        "DELAYED_CONFIRMATION": int(total_samples * 0.07),
        "BANK_DEBIT_GATEWAY_FAILURE": int(total_samples * 0.08),
        "MISSING_WEBHOOK": int(total_samples * 0.08),
        "DUPLICATE_PAYMENT": int(total_samples * 0.07),
        "REFUND_UNCERTAINTY": int(total_samples * 0.06),
        "SETTLEMENT_MISMATCH": int(total_samples * 0.06),
        "ORDER_PAYMENT_CONFLICT": int(total_samples * 0.07),
        "UNRESOLVED": int(total_samples * 0.07)
    }

    records = []
    sample_idx = 1
    for cls, count in class_distribution.items():
        for _ in range(count):
            records.append(generate_sample(cls, sample_idx))
            sample_idx += 1

    df = pd.DataFrame(records)
    df = df.sample(frac=1.0, random_state=SEED).reset_index(drop=True)

    csv_path = os.path.join(output_dir, "historical_payments.csv")
    df.to_csv(csv_path, index=False)
    print(f"Generated {len(df)} historical payment records across {len(CLASSES)} classes.", flush=True)
    print(f"Dataset saved to: {csv_path}", flush=True)
    return df

if __name__ == "__main__":
    generate_full_dataset()
