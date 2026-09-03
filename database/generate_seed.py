"""
Seed Data Generator for Payment Proof MySQL Database
Generates 550+ realistic payment records across 13 tables with realistic
distribution of normal payments and 10 specific incident scenarios.
"""

import random
import json
import hashlib
from datetime import datetime, timedelta

random.seed(42)  # For deterministic reproducibility

# ------------------------------------------------------------------------------
# Constants & Master Dictionaries
# ------------------------------------------------------------------------------

MERCHANTS = [
    {"id": "merch_flipkart_in", "name": "Flipkart India", "webhook_url": "https://api.flipkart.com/v2/webhooks/razorpay"},
    {"id": "merch_swiggy_food", "name": "Swiggy Food & Instamart", "webhook_url": "https://oms.swiggy.com/payments/callback"},
    {"id": "merch_zomato_media", "name": "Zomato Limited", "webhook_url": "https://gateway.zomato.com/pay-hook"},
    {"id": "merch_amazon_pay", "name": "Amazon India Marketplace", "webhook_url": "https://pay.amazon.in/events/webhook"},
    {"id": "merch_myntra_fash", "name": "Myntra Designs", "webhook_url": "https://payments.myntra.com/notification"},
    {"id": "merch_zepto_quick", "name": "Zepto Quick Commerce", "webhook_url": "https://backend.zeptonow.com/api/v1/payments/wh"},
    {"id": "merch_blinkit_grc", "name": "Blinkit Commerce", "webhook_url": "https://delivery.blinkit.com/hooks/pg"},
    {"id": "merch_tatacliq_ret", "name": "Tata CLiQ Luxury", "webhook_url": "https://api.tatacliq.com/webhooks/payment"},
    {"id": "merch_bookmyshow", "name": "Bigtree BookMyShow", "webhook_url": "https://secure.bookmyshow.com/wh/payment"},
    {"id": "merch_makemytrip", "name": "MakeMyTrip India", "webhook_url": "https://booking.makemytrip.com/pay/events"}
]

BANKS = [
    "HDFC_BANK", "ICICI_BANK", "STATE_BANK_OF_INDIA", "AXIS_BANK", 
    "KOTAK_MAHINDRA_BANK", "PUNJAB_NATIONAL_BANK", "YES_BANK", "INDUSIND_BANK"
]

GATEWAYS = ["RAZORPAY", "CASHFREE", "PAYU", "BILLDESK", "STRIPE"]

PAYMENT_METHODS = [
    ("UPI", ["GPAY", "PHONEPE", "PAYTM", "CRED_UPI", "BHIM", "AMAZON_UPI"]),
    ("CREDIT_CARD", ["HDFC_VISA_SIGNATURE", "ICICI_CORAL_MC", "AXIS_MAGNUS_VISA", "SBI_SIMPLYCLICK_VISA", "AMEX_PLATINUM"]),
    ("DEBIT_CARD", ["HDFC_MILLENNIA_DEBIT", "SBI_GLOBAL_RUPAY", "ICICI_SAPPHIRO_DEBIT", "AXIS_REWARDS_DEBIT"]),
    ("NET_BANKING", ["HDFC_NETBANKING", "ICICI_NETBANKING", "SBI_ONLINE", "AXIS_INTERNET_BANKING", "KOTAK_NETBANKING"]),
    ("WALLET", ["PAYTM_WALLET", "AMAZON_PAY_BALANCE", "MOBIKWIK_WALLET"]),
    ("EMI", ["HDFC_CC_NO_COST_EMI", "BAJAJ_FINSERV_EMI", "ICICI_CARDLESS_EMI"])
]

OPERATORS = [
    "operator_priya_m", "operator_arjun_k", "operator_rahul_s", 
    "operator_deepa_n", "operator_vikram_singh", "operator_ananya_roy"
]

FIRST_NAMES = ["Aarav", "Aditi", "Rohan", "Sneha", "Vikram", "Pooja", "Ankit", "Divya", "Karan", "Meera", "Siddharth", "Neha", "Abhishek", "Kavita", "Gaurav", "Swati", "Nikhil", "Tanvi", "Suresh", "Ishita"]
LAST_NAMES = ["Sharma", "Verma", "Gupta", "Malhotra", "Mehta", "Patel", "Reddy", "Iyer", "Nair", "Choudhury", "Bose", "Joshi", "Mishra", "Deshmukh", "Singhania", "Kapoor"]

# ------------------------------------------------------------------------------
# Helper Functions
# ------------------------------------------------------------------------------

def escape_sql_str(val):
    if val is None:
        return "NULL"
    s = str(val).replace("\\", "\\\\").replace("'", "''")
    return f"'{s}'"

def escape_sql_json(val):
    if val is None:
        return "NULL"
    s = json.dumps(val).replace("\\", "\\\\").replace("'", "''")
    return f"'{s}'"

def escape_sql_num(val):
    if val is None:
        return "NULL"
    return f"{val:.2f}"

def format_ts(dt):
    if dt is None:
        return "NULL"
    return f"'{dt.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}'"

def gen_hash(content):
    return hashlib.sha256(content.encode('utf-8')).hexdigest()

def random_cust():
    fn = random.choice(FIRST_NAMES)
    ln = random.choice(LAST_NAMES)
    cid = f"cust_{fn.lower()}_{ln.lower()}_{random.randint(100, 999)}"
    return cid

# ------------------------------------------------------------------------------
# Generation Logic
# ------------------------------------------------------------------------------

def generate_dataset():
    # Table accumulation lists
    payments_list = []
    events_list = []
    bank_list = []
    gateway_list = []
    merchant_order_list = []
    webhook_list = []
    settlement_list = []
    refund_list = []
    incident_list = []
    evidence_list = []
    ml_list = []
    resolution_list = []
    audit_list = []

    base_time = datetime(2026, 8, 1, 9, 0, 0)
    
    # We will generate 560 payments total:
    # 360 Normal Successful payments
    # 20 Delayed Confirmation
    # 25 Bank Debit Gateway Failure
    # 25 Gateway Success Missing Webhook
    # 20 Duplicate Payment
    # 20 Refund Uncertainty
    # 20 Settlement Mismatch
    # 20 Merchant Cancellation Before Confirmation
    # 25 Conflicting Payment States
    # 15 Genuinely Unresolved Cases
    # 10 Normal Payment False Alarm

    scenario_counts = [
        ("NORMAL_SUCCESS", 360),
        ("DELAYED_CONFIRMATION", 20),
        ("BANK_DEBIT_GATEWAY_FAILURE", 25),
        ("GATEWAY_SUCCESS_MISSING_WEBHOOK", 25),
        ("DUPLICATE_PAYMENT", 20),
        ("REFUND_UNCERTAINTY", 20),
        ("SETTLEMENT_MISMATCH", 20),
        ("MERCHANT_CANCELLATION_BEFORE_CONFIRMATION", 20),
        ("CONFLICTING_PAYMENT_STATES", 25),
        ("GENUINELY_UNRESOLVED_CASE", 15),
        ("NORMAL_PAYMENT_FALSE_ALARM", 10)
    ]

    scenario_plan = []
    for sc_type, count in scenario_counts:
        scenario_plan.extend([sc_type] * count)
    
    random.shuffle(scenario_plan)

    pay_seq = 0
    event_seq = 0
    bank_seq = 0
    gw_seq = 0
    mor_seq = 0
    wh_seq = 0
    set_seq = 0
    ref_seq = 0
    inc_seq = 0
    evi_seq = 0
    mla_seq = 0
    res_seq = 0
    aud_seq = 0

    for idx, scenario in enumerate(scenario_plan):
        pay_seq += 1
        payment_id = f"pay_{pay_seq:06d}"
        
        # Time distribution across August 2026
        time_offset_sec = idx * 4000 + random.randint(10, 1500)
        t0 = base_time + timedelta(seconds=time_offset_sec)
        
        merch = random.choice(MERCHANTS)
        merchant_id = merch["id"]
        customer_id = random_cust()
        order_id = f"ORD-2026-{pay_seq:05d}"
        
        # Amount tiers
        amt_tier = random.random()
        if amt_tier < 0.35:
            amount = round(random.uniform(99.0, 799.0), 2)
        elif amt_tier < 0.75:
            amount = round(random.uniform(800.0, 4999.0), 2)
        elif amt_tier < 0.95:
            amount = round(random.uniform(5000.0, 24999.0), 2)
        else:
            amount = round(random.uniform(25000.0, 95000.0), 2)

        pm_tuple = random.choices(PAYMENT_METHODS, weights=[0.55, 0.20, 0.10, 0.08, 0.04, 0.03])[0]
        payment_method = pm_tuple[0]
        payment_method_subtype = random.choice(pm_tuple[1])

        bank_name = random.choice(BANKS)
        gateway_name = random.choice(GATEWAYS)
        bank_ref = f"RRN{random.randint(100000000000, 999999999999)}"
        utr = f"{random.randint(400000000000, 499999999999)}" if payment_method in ['UPI', 'NET_BANKING'] else f"CARD_{random.randint(10000000, 99999999)}"
        account_last4 = f"{random.randint(1000, 9999)}"
        gw_txn_id = f"txn_gw_{pay_seq:06d}_{random.randint(100, 999)}"
        gw_order_id = f"order_gw_{pay_seq:06d}"
        
        fee = round(amount * 0.018, 2)
        tax = round(fee * 0.18, 2)
        net_settled = round(amount - fee - tax, 2)

        # ----------------------------------------------------------------------
        # SCENARIO SPECIFIC MAPPINGS
        # ----------------------------------------------------------------------
        
        if scenario == "NORMAL_SUCCESS":
            p_status = "SUCCESS"
            completed_at = t0 + timedelta(seconds=random.randint(2, 5))
            
            # Bank
            b_status = "SUCCESS"
            b_debited = amount
            b_code = "00"
            b_msg = "TRANSACTION_APPROVED_NPCI_ACK"
            b_latency = random.randint(180, 450)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            # Gateway
            g_auth = "AUTHORIZED"
            g_cap = "CAPTURED"
            g_status = "SUCCESS"
            g_code = None
            g_desc = None
            g_latency = b_latency + random.randint(100, 250)
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            # Merchant OMS
            m_status = "PAID"
            m_fulfill = "DELIVERED" if (base_time + timedelta(days=28)) > t0 else "PROCESSING"
            m_cancel_reason = None
            m_time = g_time + timedelta(milliseconds=random.randint(80, 300))
            
            # Webhook
            w_status = "DELIVERED"
            w_http = 200
            w_attempts = 1
            w_resp = '{"status": "ACK_RECEIVED", "order_id": "' + order_id + '"}'
            w_latency = random.randint(80, 200)
            w_time = g_time + timedelta(milliseconds=100)
            
            # Settlement
            s_status = "SETTLED"
            s_batch = f"SETTLE_BATCH_{t0.strftime('%Y%m%d')}_01"
            s_time = t0 + timedelta(days=1, hours=random.randint(2, 6))
            s_utr = f"SETTLE_UTR_{random.randint(100000000, 999999999)}"

            # Refund
            has_refund = False
            has_incident = False

        elif scenario == "DELAYED_CONFIRMATION":
            # Bank debit took 120+ seconds, Gateway delayed callback, Merchant eventually updated
            p_status = "SUCCESS"
            completed_at = t0 + timedelta(seconds=random.randint(120, 240))
            
            b_status = "SUCCESS"
            b_debited = amount
            b_code = "00"
            b_msg = "LATE_DEBIT_ACK_FROM_ISSUER"
            b_latency = random.randint(110000, 180000)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "AUTHORIZED"
            g_cap = "CAPTURED"
            g_status = "SUCCESS"
            g_code = "ASYNC_LATE_CAPTURE"
            g_desc = "Capture finalized after secondary webhook poll"
            g_latency = b_latency + 2500
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "PAID"
            m_fulfill = "PROCESSING"
            m_cancel_reason = None
            m_time = g_time + timedelta(seconds=5)
            
            w_status = "DELIVERED"
            w_http = 200
            w_attempts = 2
            w_resp = '{"status": "DELAYED_ACK_ACCEPTED"}'
            w_latency = 350
            w_time = g_time + timedelta(seconds=2)
            
            s_status = "SETTLED"
            s_batch = f"SETTLE_BATCH_{t0.strftime('%Y%m%d')}_02"
            s_time = t0 + timedelta(days=1, hours=8)
            s_utr = f"SETTLE_UTR_{random.randint(100000000, 999999999)}"

            has_refund = False
            has_incident = True
            inc_type = "DELAYED_CONFIRMATION"
            inc_sev = "LOW"
            inc_status = "RESOLVED"
            inc_trigger = "AUTOMATED_RECONCILIATION"
            inc_title = f"Delayed PSP Confirmation for {order_id}"
            inc_desc = f"Bank debit took {b_latency/1000:.1f}s to acknowledge. Reconciliation engine auto-synced gateway state."
            mla_cause = "PSP_DOWNSTREAM_LATENCY_SPIKE"
            mla_score = 0.4200
            mla_conf = 0.9400
            mla_action = "NO_ACTION_REQUIRED"
            res_action = "NO_DISCREPANCY_FOUND"
            res_type = "AUTOMATED_RULE_ENGINE"
            res_by = "SYSTEM_AUTO_RECON"
            res_notes = "Payment confirmed asynchronously via delayed PSP telemetry. Order fulfilled."
            res_liability = "PLATFORM_LOSS_NONE"

        elif scenario == "BANK_DEBIT_GATEWAY_FAILURE":
            # Classic ghost debit: Bank debited customer, but Gateway timed out / failed, merchant cart cancelled!
            p_status = "FLAGGED"
            completed_at = None
            
            b_status = "DEBITED"
            b_debited = amount
            b_code = "00"
            b_msg = "FUNDS_DEBITED_FROM_REMITTER"
            b_latency = random.randint(300, 600)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "TIMEOUT"
            g_cap = "FAILED"
            g_status = "FAILED"
            g_code = "GATEWAY_TIMEOUT_SOCKET_CLOSED"
            g_desc = "Client disconnect before switch callback could be delivered"
            g_latency = random.randint(45000, 60000)
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "CANCELLED"
            m_fulfill = "CANCELLED"
            m_cancel_reason = "PAYMENT_GATEWAY_REPORTED_FAILURE"
            m_time = g_time + timedelta(seconds=2)
            
            w_status = "DELIVERED"
            w_http = 200
            w_attempts = 1
            w_resp = '{"ack": "RECEIVED_FAILURE_EVENT"}'
            w_latency = 120
            w_time = g_time + timedelta(milliseconds=200)
            
            s_status = "ON_HOLD"
            s_batch = None
            s_time = None
            s_utr = None

            has_refund = True
            ref_amt = amount
            ref_reason = "INCIDENT_AUTO_REFUND"
            ref_status = "PROCESSED"
            ref_bank_status = "CREDITED_TO_CUSTOMER"

            has_incident = True
            inc_type = "BANK_DEBIT_GATEWAY_FAILURE"
            inc_sev = "CRITICAL"
            inc_status = "RESOLVED"
            inc_trigger = "CUSTOMER_TICKET"
            inc_title = f"Bank Debited but Gateway Reported Failure on {order_id}"
            inc_desc = f"Customer account debited INR {amount:.2f} (UTR {utr}), but Gateway timed out. Merchant marked order as CANCELLED."
            mla_cause = "GATEWAY_DOWNSTREAM_DISCONNECT_POST_DEBIT"
            mla_score = 0.9650
            mla_conf = 0.9820
            mla_action = "AUTO_REFUND_CUSTOMER"
            res_action = "CUSTOMER_REFUNDED"
            res_type = "ML_SUPERVISED_AUTO"
            res_by = "SYSTEM_AUTO_RECON"
            res_notes = f"Initiated full customer refund of INR {amount:.2f} via source account reversal due to merchant cart expiry."
            res_liability = "GATEWAY"

        elif scenario == "GATEWAY_SUCCESS_MISSING_WEBHOOK":
            # Bank success, Gateway captured, but Webhook failed 3 times! Merchant left order unfulfilled/cancelled
            p_status = "DISPUTED"
            completed_at = t0 + timedelta(seconds=3)
            
            b_status = "SUCCESS"
            b_debited = amount
            b_code = "00"
            b_msg = "DEBIT_SUCCESSFUL"
            b_latency = random.randint(200, 450)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "AUTHORIZED"
            g_cap = "CAPTURED"
            g_status = "SUCCESS"
            g_code = None
            g_desc = None
            g_latency = b_latency + 150
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "PENDING_PAYMENT"
            m_fulfill = "UNFULFILLED"
            m_cancel_reason = "NO_WEBHOOK_RECEIVED"
            m_time = g_time + timedelta(minutes=5)
            
            w_status = "FAILED"
            w_http = 504
            w_attempts = 3
            w_resp = "<html>504 Gateway Time-out: Merchant webhook endpoint unreachable</html>"
            w_latency = 5000
            w_time = g_time + timedelta(seconds=2)
            
            s_status = "ON_HOLD"
            s_batch = f"SETTLE_BATCH_{t0.strftime('%Y%m%d')}_HOLD"
            s_time = None
            s_utr = None

            has_refund = False
            has_incident = True
            inc_type = "GATEWAY_SUCCESS_MISSING_WEBHOOK"
            inc_sev = "HIGH"
            inc_status = "RESOLVED"
            inc_trigger = "WEBHOOK_MONITOR"
            inc_title = f"Captured Payment Webhook Dropped for {order_id}"
            inc_desc = f"Gateway captured INR {amount:.2f} successfully, but merchant webhook failed with HTTP 504. Merchant OMS unfulfilled."
            mla_cause = "MERCHANT_WEBHOOK_ENDPOINT_OUTAGE"
            mla_score = 0.8840
            mla_conf = 0.9610
            mla_action = "RESEND_WEBHOOK"
            res_action = "WEBHOOK_RESENT_AND_FULFILLED"
            res_type = "OPERATOR_MANUAL_OVERRIDE"
            res_by = random.choice(OPERATORS)
            res_notes = "Manually verified gateway capture and replayed authoritative webhook payload. Merchant confirmed order fulfillment."
            res_liability = "MERCHANT"

        elif scenario == "DUPLICATE_PAYMENT":
            # Customer retried after slow screen, leading to duplicate debits
            p_status = "DISPUTED"
            completed_at = t0 + timedelta(seconds=15)
            
            b_status = "SUCCESS"
            b_debited = amount
            b_code = "00"
            b_msg = "DUPLICATE_AUTH_DEBITED_TWICE"
            b_latency = random.randint(300, 550)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "AUTHORIZED"
            g_cap = "CAPTURED"
            g_status = "SUCCESS"
            g_code = "DUPLICATE_ORDER_CHARGE"
            g_desc = "Secondary charge detected for identical merchant order ID"
            g_latency = b_latency + 200
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "PAID"
            m_fulfill = "DELIVERED"
            m_cancel_reason = None
            m_time = g_time + timedelta(seconds=2)
            
            w_status = "DELIVERED"
            w_http = 200
            w_attempts = 1
            w_resp = '{"status": "ORDER_ALREADY_PAID_SECOND_PAYMENT_LOGGED"}'
            w_latency = 180
            w_time = g_time + timedelta(milliseconds=200)
            
            s_status = "ON_HOLD"
            s_batch = None
            s_time = None
            s_utr = None

            has_refund = True
            ref_amt = amount
            ref_reason = "DUPLICATE_PAYMENT"
            ref_status = "PROCESSED"
            ref_bank_status = "CREDITED_TO_CUSTOMER"

            has_incident = True
            inc_type = "DUPLICATE_PAYMENT"
            inc_sev = "HIGH"
            inc_status = "RESOLVED"
            inc_trigger = "AUTOMATED_RECONCILIATION"
            inc_title = f"Duplicate Charge Detected for Order {order_id}"
            inc_desc = f"Order {order_id} received two distinct successful charges of INR {amount:.2f}. Duplicate charge flagged for reversal."
            mla_cause = "USER_RETRY_RACE_CONDITION"
            mla_score = 0.9120
            mla_conf = 0.9910
            mla_action = "AUTO_REFUND_CUSTOMER"
            res_action = "DUPLICATE_REVERSED"
            res_type = "AUTOMATED_RULE_ENGINE"
            res_by = "SYSTEM_AUTO_RECON"
            res_notes = f"Auto-refunded duplicate transaction {payment_id} of INR {amount:.2f} back to customer account."
            res_liability = "PLATFORM_LOSS_NONE"

        elif scenario == "REFUND_UNCERTAINTY":
            # Refund initiated in gateway, but bank reversal failed or timed out in clearing network
            p_status = "REFUNDED"
            completed_at = t0 + timedelta(minutes=10)
            
            b_status = "SUCCESS"
            b_debited = amount
            b_code = "00"
            b_msg = "ORIGINAL_DEBIT_CONFIRMED"
            b_latency = random.randint(250, 450)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "AUTHORIZED"
            g_cap = "CAPTURED"
            g_status = "SUCCESS"
            g_code = None
            g_desc = None
            g_latency = b_latency + 150
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "REFUNDED"
            m_fulfill = "RETURNED"
            m_cancel_reason = "CUSTOMER_RETURN_REQUEST"
            m_time = g_time + timedelta(days=2)
            
            w_status = "DELIVERED"
            w_http = 200
            w_attempts = 1
            w_resp = '{"refund_acknowledged": true}'
            w_latency = 190
            w_time = g_time + timedelta(milliseconds=200)
            
            s_status = "REVERSED"
            s_batch = f"SETTLE_BATCH_{t0.strftime('%Y%m%d')}_REV"
            s_time = t0 + timedelta(days=2, hours=4)
            s_utr = f"SETTLE_REV_UTR_{random.randint(100000000, 999999999)}"

            has_refund = True
            ref_amt = amount
            ref_reason = "CUSTOMER_DISPUTE"
            ref_status = "MANUAL_INTERVENTION_REQUIRED"
            ref_bank_status = "AWAITING_ACK"

            has_incident = True
            inc_type = "REFUND_UNCERTAINTY"
            inc_sev = "HIGH"
            inc_status = "IN_REVIEW"
            inc_trigger = "MERCHANT_DISPUTE"
            inc_title = f"Refund Settlement Desynchronization on {order_id}"
            inc_desc = f"Gateway processed refund for INR {amount:.2f}, but issuer bank clearing network has not acknowledged ARN credit."
            mla_cause = "ISSUER_BANK_CLEARING_HOUSE_DELINK"
            mla_score = 0.7950
            mla_conf = 0.8840
            mla_action = "MANUAL_BANK_ESCALATION"
            res_action = None

        elif scenario == "SETTLEMENT_MISMATCH":
            # Gateway captured amount != Settlement ledger batch amount (e.g. calculation discrepancy or fee overcharge)
            p_status = "SUCCESS"
            completed_at = t0 + timedelta(seconds=4)
            
            b_status = "SUCCESS"
            b_debited = amount
            b_code = "00"
            b_msg = "SUCCESSFUL_DEBIT"
            b_latency = random.randint(220, 480)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "AUTHORIZED"
            g_cap = "CAPTURED"
            g_status = "SUCCESS"
            g_code = None
            g_desc = None
            g_latency = b_latency + 180
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "PAID"
            m_fulfill = "DELIVERED"
            m_cancel_reason = None
            m_time = g_time + timedelta(seconds=1)
            
            w_status = "DELIVERED"
            w_http = 200
            w_attempts = 1
            w_resp = '{"ack": true}'
            w_latency = 140
            w_time = g_time + timedelta(milliseconds=150)
            
            # Artificial mismatch in settlement
            s_status = "DISCREPANCY"
            s_batch = f"SETTLE_BATCH_{t0.strftime('%Y%m%d')}_ERR"
            s_time = t0 + timedelta(days=1, hours=3)
            s_utr = f"SETTLE_UTR_{random.randint(100000000, 999999999)}"
            # Net settled is missing 15% unexpectedly
            net_settled = round(net_settled * 0.85, 2)

            has_refund = False
            has_incident = True
            inc_type = "SETTLEMENT_MISMATCH"
            inc_sev = "MEDIUM"
            inc_status = "RESOLVED"
            inc_trigger = "SETTLEMENT_AUDITOR"
            inc_title = f"Settlement Payout Variance for Batch {s_batch}"
            inc_desc = f"Expected net settlement INR {amount - fee - tax:.2f}, but batch computed INR {net_settled:.2f} (Variance of INR {amount - fee - tax - net_settled:.2f})."
            mla_cause = "MDR_TIER_FEE_CALCULATION_DRIFT"
            mla_score = 0.6800
            mla_conf = 0.9320
            mla_action = "FORCE_SETTLE_MERCHANT"
            res_action = "TRANSACTION_SETTLED_MANUALLY"
            res_type = "OPERATOR_MANUAL_OVERRIDE"
            res_by = random.choice(OPERATORS)
            res_notes = f"Re-calculated MDR fees and credited adjustment delta of INR {amount - fee - tax - net_settled:.2f} to merchant {merchant_id}."
            res_liability = "GATEWAY"

        elif scenario == "MERCHANT_CANCELLATION_BEFORE_CONFIRMATION":
            # Cart timeout at 5:00, 3DS authentication finished at 5:12, bank debited
            p_status = "FLAGGED"
            completed_at = t0 + timedelta(minutes=5, seconds=12)
            
            b_status = "SUCCESS"
            b_debited = amount
            b_code = "00"
            b_msg = "3DS_OTP_AUTHENTICATED_LATE"
            b_latency = 312000
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "AUTHORIZED"
            g_cap = "CAPTURED"
            g_status = "SUCCESS"
            g_code = "LATE_AUTHORIZATION_SUCCESS"
            g_desc = "3DS verification completed after merchant timeout window"
            g_latency = b_latency + 800
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "CANCELLED"
            m_fulfill = "CANCELLED"
            m_cancel_reason = "PAYMENT_WINDOW_EXPIRED"
            m_time = t0 + timedelta(minutes=5)  # Merchant cancelled BEFORE bank capture
            
            w_status = "DELIVERED"
            w_http = 200
            w_attempts = 1
            w_resp = '{"error": "ORDER_ALREADY_CANCELLED_UNABLE_TO_FULFILL"}'
            w_latency = 220
            w_time = g_time + timedelta(milliseconds=300)
            
            s_status = "ON_HOLD"
            s_batch = None
            s_time = None
            s_utr = None

            has_refund = True
            ref_amt = amount
            ref_reason = "ORDER_CANCELLED"
            ref_status = "PROCESSED"
            ref_bank_status = "CREDITED_TO_CUSTOMER"

            has_incident = True
            inc_type = "MERCHANT_CANCELLATION_BEFORE_CONFIRMATION"
            inc_sev = "HIGH"
            inc_status = "RESOLVED"
            inc_trigger = "MERCHANT_DISPUTE"
            inc_title = f"Late Authorization on Cancelled Order {order_id}"
            inc_desc = f"Merchant inventory session timed out at 5m00s, but customer completed 3DS at 5m12s. Bank debited INR {amount:.2f}."
            mla_cause = "PREMATURE_MERCHANT_SESSION_EXPIRY"
            mla_score = 0.8920
            mla_conf = 0.9750
            mla_action = "AUTO_REFUND_CUSTOMER"
            res_action = "CUSTOMER_REFUNDED"
            res_type = "ML_SUPERVISED_AUTO"
            res_by = "SYSTEM_AUTO_RECON"
            res_notes = f"Auto-refunded INR {amount:.2f} because merchant inventory was released prior to late 3DS capture."
            res_liability = "MERCHANT"

        elif scenario == "CONFLICTING_PAYMENT_STATES":
            # Multi-system contradiction (Bank=SUCCESS, Gateway=PENDING, Merchant=CANCELLED, Webhook=DROPPED)
            p_status = "FLAGGED"
            completed_at = None
            
            b_status = "SUCCESS"
            b_debited = amount
            b_code = "00"
            b_msg = "DEBIT_PROCESSED_AC_CONFIRMED"
            b_latency = random.randint(350, 700)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "TIMEOUT"
            g_cap = "PENDING"
            g_status = "PENDING"
            g_code = "ASYNC_PSP_CALLBACK_MISSING"
            g_desc = "Awaiting NPCI switch confirmation packet"
            g_latency = 90000
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "CANCELLED"
            m_fulfill = "CANCELLED"
            m_cancel_reason = "SESSION_TIMEOUT_NO_PROOF"
            m_time = t0 + timedelta(minutes=6)
            
            w_status = "DROPPED"
            w_http = None
            w_attempts = 0
            w_resp = None
            w_latency = None
            w_time = t0 + timedelta(minutes=7)
            
            s_status = "ON_HOLD"
            s_batch = None
            s_time = None
            s_utr = None

            has_refund = True
            ref_amt = amount
            ref_reason = "INCIDENT_AUTO_REFUND"
            ref_status = "PROCESSED"
            ref_bank_status = "CREDITED_TO_CUSTOMER"

            has_incident = True
            inc_type = "CONFLICTING_PAYMENT_STATES"
            inc_sev = "CRITICAL"
            inc_status = "RESOLVED"
            inc_trigger = "AUTOMATED_RECONCILIATION"
            inc_title = f"Multi-Party State Contradiction for {order_id}"
            inc_desc = f"Bank debited INR {amount:.2f} (SUCCESS), Gateway stuck in PENDING, Merchant marked CANCELLED, Webhook DROPPED."
            mla_cause = "MULTI_SYSTEM_STATE_DESYNCHRONIZATION"
            mla_score = 0.9850
            mla_conf = 0.9920
            mla_action = "AUTO_REFUND_CUSTOMER"
            res_action = "CUSTOMER_REFUNDED"
            res_type = "ML_SUPERVISED_AUTO"
            res_by = "SYSTEM_AUTO_RECON"
            res_notes = f"Reconciled orphaned bank debit INR {amount:.2f} by issuing full credit reversal to customer."
            res_liability = "BANK"

        elif scenario == "GENUINELY_UNRESOLVED_CASE":
            # Complex, open forensic case with conflicting claims
            p_status = "DISPUTED"
            completed_at = t0 + timedelta(seconds=12)
            
            b_status = "REVERSED"
            b_debited = amount
            b_code = "U69"
            b_msg = "CHARGEBACK_RAISED_UNAUTHORIZED_TXN"
            b_latency = random.randint(400, 800)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "AUTHORIZED"
            g_cap = "CAPTURED"
            g_status = "SUCCESS"
            g_code = "CHARGEBACK_CLAIM_LODGED"
            g_desc = "Issuer initiated first chargeback stage"
            g_latency = b_latency + 200
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "PAID"
            m_fulfill = "DELIVERED"
            m_cancel_reason = None
            m_time = g_time + timedelta(seconds=2)
            
            w_status = "DELIVERED"
            w_http = 200
            w_attempts = 1
            w_resp = '{"ack": true}'
            w_latency = 160
            w_time = g_time + timedelta(milliseconds=200)
            
            s_status = "ON_HOLD"
            s_batch = f"SETTLE_BATCH_{t0.strftime('%Y%m%d')}_DISPUTE"
            s_time = None
            s_utr = None

            has_refund = False
            has_incident = True
            inc_type = "GENUINELY_UNRESOLVED_CASE"
            inc_sev = "CRITICAL"
            inc_status = "ESCALATED_TO_BANK"
            inc_trigger = "MERCHANT_DISPUTE"
            inc_title = f"Contested Chargeback & Ledger Discrepancy on {order_id}"
            inc_desc = f"Customer claims unauthorized transaction; Merchant has signed Proof-of-Delivery. Bank reversed funds without gateway clearance."
            mla_cause = "CONTESTED_FIRST_PARTY_FRAUD_OR_LEDGER_MISMATCH"
            mla_score = 0.9410
            mla_conf = 0.5820  # Low confidence indicates high ambiguity
            mla_action = "MANUAL_BANK_ESCALATION"
            res_action = None  # No resolution yet!

        elif scenario == "NORMAL_PAYMENT_FALSE_ALARM":
            # Normal payment where customer opened ticket but everything was fine
            p_status = "SUCCESS"
            completed_at = t0 + timedelta(seconds=3)
            
            b_status = "SUCCESS"
            b_debited = amount
            b_code = "00"
            b_msg = "SUCCESSFUL_DEBIT"
            b_latency = random.randint(190, 420)
            b_time = t0 + timedelta(milliseconds=b_latency)
            
            g_auth = "AUTHORIZED"
            g_cap = "CAPTURED"
            g_status = "SUCCESS"
            g_code = None
            g_desc = None
            g_latency = b_latency + 120
            g_time = t0 + timedelta(milliseconds=g_latency)
            
            m_status = "PAID"
            m_fulfill = "DELIVERED"
            m_cancel_reason = None
            m_time = g_time + timedelta(seconds=1)
            
            w_status = "DELIVERED"
            w_http = 200
            w_attempts = 1
            w_resp = '{"ack": true}'
            w_latency = 130
            w_time = g_time + timedelta(milliseconds=100)
            
            s_status = "SETTLED"
            s_batch = f"SETTLE_BATCH_{t0.strftime('%Y%m%d')}_01"
            s_time = t0 + timedelta(days=1, hours=3)
            s_utr = f"SETTLE_UTR_{random.randint(100000000, 999999999)}"

            has_refund = False
            has_incident = True
            inc_type = "NORMAL_PAYMENT_FALSE_ALARM"
            inc_sev = "LOW"
            inc_status = "RESOLVED"
            inc_trigger = "CUSTOMER_TICKET"
            inc_title = f"Customer Query Regarding SMS Confirmation on {order_id}"
            inc_desc = "Customer inquired about missing bank SMS alert. Telemetry confirms all 4 systems are 100% synchronized."
            mla_cause = "EXTERNAL_SMS_GATEWAY_DELIVERY_LAG"
            mla_score = 0.0820
            mla_conf = 0.9980
            mla_action = "NO_ACTION_REQUIRED"
            res_action = "NO_DISCREPANCY_FOUND"
            res_type = "AUTOMATED_RULE_ENGINE"
            res_by = "SYSTEM_AUTO_RECON"
            res_notes = "Verified all 4 systems: Bank (00 Success), Gateway (Captured), Merchant (Paid/Delivered), Webhook (HTTP 200)."
            res_liability = "PLATFORM_LOSS_NONE"

        # ----------------------------------------------------------------------
        # 1. PAYMENTS ROW
        # ----------------------------------------------------------------------
        client_ip = f"103.{random.randint(10, 250)}.{random.randint(1, 250)}.{random.randint(1, 250)}"
        user_agent = "Mozilla/5.0 (Linux; Android 14; Mobile) Chrome/128.0.0.0"
        
        payments_list.append((
            payment_id, merchant_id, customer_id, order_id, amount, "INR",
            payment_method, payment_method_subtype, p_status,
            client_ip, user_agent, t0, completed_at, completed_at or t0
        ))

        # ----------------------------------------------------------------------
        # 2. PAYMENT_EVENTS ROWS
        # ----------------------------------------------------------------------
        event_seq += 1
        events_list.append((
            f"pevt_{event_seq:07d}", payment_id, "PAYMENT_INITIATED", None, "INITIATED",
            "CLIENT_SDK", {"client_ip": client_ip, "sdk_version": "v3.8.2"}, t0
        ))
        
        event_seq += 1
        events_list.append((
            f"pevt_{event_seq:07d}", payment_id, "AUTHENTICATION_REQUESTED", "INITIATED", "PENDING",
            "GATEWAY_ENGINE", {"gateway": gateway_name, "auth_mode": payment_method}, t0 + timedelta(milliseconds=100)
        ))

        if b_status in ["SUCCESS", "DEBITED"]:
            event_seq += 1
            events_list.append((
                f"pevt_{event_seq:07d}", payment_id, "BANK_DEBIT_ACKNOWLEDGED", "PENDING", "PENDING",
                "BANK_CONNECTOR", {"bank": bank_name, "utr": utr, "response_code": b_code}, b_time
            ))

        if g_cap == "CAPTURED":
            event_seq += 1
            events_list.append((
                f"pevt_{event_seq:07d}", payment_id, "PAYMENT_CAPTURED", "PENDING", "SUCCESS",
                "GATEWAY_ENGINE", {"captured_amount": amount, "gateway_txn_id": gw_txn_id}, g_time
            ))
        elif g_status == "FAILED":
            event_seq += 1
            events_list.append((
                f"pevt_{event_seq:07d}", payment_id, "PAYMENT_FAILED", "PENDING", "FAILED",
                "GATEWAY_ENGINE", {"error_code": g_code, "error_desc": g_desc}, g_time
            ))

        # ----------------------------------------------------------------------
        # 3. BANK_RECORDS ROW
        # ----------------------------------------------------------------------
        bank_seq += 1
        bank_raw = {
            "cbs_node": f"cbs-{bank_name.lower()}-cluster-04",
            "rrn": bank_ref,
            "npci_txn_id": utr,
            "auth_code": f"AUTH{random.randint(100000, 999999)}"
        }
        bank_list.append((
            f"bnk_{bank_seq:07d}", payment_id, bank_name, bank_ref, utr, account_last4,
            b_status, b_debited, "INR", b_code, b_msg, b_latency, b_time, bank_raw, t0
        ))

        # ----------------------------------------------------------------------
        # 4. GATEWAY_RECORDS ROW
        # ----------------------------------------------------------------------
        gw_seq += 1
        gw_raw = {
            "engine": gateway_name,
            "latency_breakdown": {"dns_ms": 12, "tls_ms": 45, "psp_ms": g_latency},
            "risk_verdict": "CLEAN",
            "acquirer_reference": bank_ref
        }
        gateway_list.append((
            f"gw_{gw_seq:07d}", payment_id, gateway_name, gw_txn_id, gw_order_id,
            g_auth, g_cap, g_status, amount if g_auth == "AUTHORIZED" else 0.0,
            amount if g_cap == "CAPTURED" else 0.0, fee, tax,
            g_code, g_desc, g_latency, g_time, gw_raw, t0
        ))

        # ----------------------------------------------------------------------
        # 5. MERCHANT_ORDER_RECORDS ROW
        # ----------------------------------------------------------------------
        mor_seq += 1
        merchant_order_list.append((
            f"mor_{mor_seq:07d}", payment_id, merchant_id, order_id,
            m_status, m_fulfill, amount, "INR", m_cancel_reason,
            f"Standard checkout delivery to Pin 5600{random.randint(10, 99)}",
            m_time, t0
        ))

        # ----------------------------------------------------------------------
        # 6. WEBHOOK_RECORDS ROW
        # ----------------------------------------------------------------------
        wh_seq += 1
        wh_payload = {
            "event": "payment.captured" if g_cap == "CAPTURED" else "payment.failed",
            "payment_id": payment_id,
            "order_id": order_id,
            "amount": amount,
            "currency": "INR",
            "status": g_status.lower()
        }
        wh_hash = gen_hash(json.dumps(wh_payload))
        webhook_list.append((
            f"wh_{wh_seq:07d}", payment_id, merchant_id, wh_payload["event"],
            merch["webhook_url"], w_attempts, 3, w_status, w_http, w_latency,
            wh_hash, wh_payload, w_resp, w_time, w_time + timedelta(seconds=10) if w_attempts > 1 else w_time,
            None, t0
        ))

        # ----------------------------------------------------------------------
        # 7. SETTLEMENT_RECORDS ROW
        # ----------------------------------------------------------------------
        set_seq += 1
        settlement_list.append((
            f"set_{set_seq:07d}", payment_id, merchant_id, s_batch,
            amount, fee, tax, net_settled, "INR", s_status, s_utr,
            f"XXXXXX{random.randint(1000, 9999)}", s_time, t0, s_time or t0
        ))

        # ----------------------------------------------------------------------
        # 8. REFUND_RECORDS ROW (if applicable)
        # ----------------------------------------------------------------------
        if has_refund:
            ref_seq += 1
            refund_id = f"ref_{ref_seq:07d}"
            gw_ref_id = f"rfnd_gw_{ref_seq:06d}"
            ref_arn = f"ARN_{random.randint(100000000000, 999999999999)}"
            ref_raw = {"gateway_ack": True, "bank_clearing_channel": "INSTANT_UPI_REVERSAL" if payment_method == "UPI" else "NEFT_BULK"}
            
            refund_list.append((
                refund_id, payment_id, merchant_id, gw_ref_id, ref_arn,
                ref_amt, "INR", ref_reason, "INSTANT" if payment_method == "UPI" else "NORMAL",
                ref_status, ref_bank_status, t0 + timedelta(minutes=5),
                t0 + timedelta(minutes=6) if ref_status == "PROCESSED" else None,
                ref_raw
            ))

        # ----------------------------------------------------------------------
        # 9. INCIDENT_CASES & CONNECTED ENTITIES
        # ----------------------------------------------------------------------
        if has_incident:
            inc_seq += 1
            incident_id = f"inc_{inc_seq:07d}"
            assigned_op = random.choice(OPERATORS)
            inc_opened = t0 + timedelta(minutes=2)
            inc_resolved = inc_opened + timedelta(minutes=15) if inc_status == "RESOLVED" else None
            
            incident_list.append((
                incident_id, payment_id, inc_type, inc_sev, inc_status,
                inc_trigger, assigned_op, inc_title, inc_desc,
                inc_opened, inc_resolved, inc_resolved or inc_opened
            ))

            # 10. INVESTIGATION_EVIDENCE (2 to 3 evidence artifacts per incident)
            evi_seq += 1
            evidence_list.append((
                f"evi_{evi_seq:07d}", incident_id, payment_id, "CORE_BANKING_LOG", "JSON_TELEMETRY",
                f"/telemetry/bank/{bank_name.lower()}/{payment_id}.json",
                json.dumps({"cbs_status": b_status, "utr": utr, "response_code": b_code, "latency_ms": b_latency}),
                gen_hash(f"bank_evi_{payment_id}"), inc_opened + timedelta(seconds=10)
            ))

            evi_seq += 1
            evidence_list.append((
                f"evi_{evi_seq:07d}", incident_id, payment_id, "GATEWAY_TELEMETRY", "JSON_TELEMETRY",
                f"/telemetry/gateway/{gateway_name.lower()}/{payment_id}.json",
                json.dumps({"gateway_status": g_status, "auth_status": g_auth, "capture_status": g_cap, "error": g_code}),
                gen_hash(f"gw_evi_{payment_id}"), inc_opened + timedelta(seconds=20)
            ))

            evi_seq += 1
            evidence_list.append((
                f"evi_{evi_seq:07d}", incident_id, payment_id, "MERCHANT_OMS_LOG", "HTTP_TRACE",
                f"/telemetry/merchant/{merchant_id}/{order_id}.log",
                f"OMS_CHECKOUT_EVENT: order_id={order_id}, status={m_status}, fulfillment={m_fulfill}, reason={m_cancel_reason}",
                gen_hash(f"mor_evi_{payment_id}"), inc_opened + timedelta(seconds=30)
            ))

            # 11. ML_ASSESSMENTS
            mla_seq += 1
            assessment_id = f"mla_{mla_seq:07d}"
            feat_snap = {
                "amount": amount,
                "payment_method": payment_method,
                "bank_latency_ms": b_latency,
                "gateway_latency_ms": g_latency,
                "webhook_attempts": w_attempts,
                "webhook_http_status": w_http,
                "bank_status": b_status,
                "gateway_status": g_status,
                "merchant_status": m_status,
                "is_amount_matched": (b_debited == amount) if b_debited else False
            }
            exp_json = {
                "primary_driver": mla_cause,
                "risk_exposure_inr": amount if b_status in ["SUCCESS", "DEBITED"] and m_status != "PAID" else 0.0,
                "shap_values": {
                    "bank_gateway_status_divergence": 0.45,
                    "merchant_oms_timeout_flag": 0.32,
                    "webhook_drop_probability": 0.23
                }
            }
            ml_list.append((
                assessment_id, incident_id, payment_id, "reconcile-net-v2.1",
                mla_cause, mla_score, mla_conf, mla_action,
                feat_snap, exp_json, inc_opened + timedelta(seconds=45)
            ))

            # 12. RESOLUTIONS (if resolved)
            if inc_status == "RESOLVED" and res_action is not None:
                res_seq += 1
                resolution_id = f"res_{res_seq:07d}"
                financial_impact = amount if res_action in ["CUSTOMER_REFUNDED", "MERCHANT_CREDITED", "DUPLICATE_REVERSED"] else 0.0
                resolution_list.append((
                    resolution_id, incident_id, payment_id, res_action,
                    res_type, res_by, res_notes, financial_impact,
                    res_liability, inc_resolved
                ))

        # ----------------------------------------------------------------------
        # 13. AUDIT_EVENTS (1 to 4 audit events per payment)
        # ----------------------------------------------------------------------
        aud_seq += 1
        audit_list.append((
            f"aud_{aud_seq:07d}", "PAYMENTS", payment_id, "PAYMENT_CREATED",
            "SYSTEM", "INGESTION_API", None, {"status": "INITIATED", "amount": amount},
            client_ip, t0
        ))

        if has_incident:
            aud_seq += 1
            audit_list.append((
                f"aud_{aud_seq:07d}", "INCIDENT_CASES", incident_id, "INCIDENT_OPENED",
                "SYSTEM", "RECONCILIATION_ENGINE", None, {"type": inc_type, "severity": inc_sev},
                "10.0.4.12", inc_opened
            ))

            aud_seq += 1
            audit_list.append((
                f"aud_{aud_seq:07d}", "ML_ASSESSMENTS", assessment_id, "ML_INFERENCE_RECORDED",
                "ML_SERVICE", "RECONCILE_MODEL_WORKER", None, {"predicted_root_cause": mla_cause, "confidence": mla_conf},
                "10.0.8.99", inc_opened + timedelta(seconds=45)
            ))

            if inc_status == "RESOLVED" and res_action is not None:
                aud_seq += 1
                audit_list.append((
                    f"aud_{aud_seq:07d}", "RESOLUTIONS", resolution_id, "RESOLUTION_EXECUTED",
                    "WORKFLOW_ENGINE" if res_type != "OPERATOR_MANUAL_OVERRIDE" else "OPERATOR_USER",
                    res_by, {"case_status": "OPEN"}, {"case_status": "RESOLVED", "action": res_action},
                    "10.0.2.15", inc_resolved
                ))

    return {
        "payments": payments_list,
        "payment_events": events_list,
        "bank_records": bank_list,
        "gateway_records": gateway_list,
        "merchant_order_records": merchant_order_list,
        "webhook_records": webhook_list,
        "settlement_records": settlement_list,
        "refund_records": refund_list,
        "incident_cases": incident_list,
        "investigation_evidence": evidence_list,
        "ml_assessments": ml_list,
        "resolutions": resolution_list,
        "audit_events": audit_list
    }

# ------------------------------------------------------------------------------
# SQL Formatter & Output Writer
# ------------------------------------------------------------------------------

def write_seed_sql(data, filename="seed.sql"):
    with open(filename, "w", encoding="utf-8") as f:
        f.write("-- =============================================================================\n")
        f.write("-- PAYMENT PROOF: Seed Data (550+ Payment Records across 13 Entities)\n")
        f.write("-- Contains realistic distribution of normal payments & 10 forensic incident types\n")
        f.write("-- =============================================================================\n\n")
        f.write("SET FOREIGN_KEY_CHECKS = 0;\n\n")

        # 1. PAYMENTS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 1. PAYMENTS ({len(data['payments'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO payments (payment_id, merchant_id, customer_id, order_id, amount, currency, payment_method, payment_method_subtype, status, client_ip, user_agent, initiated_at, completed_at, updated_at) VALUES\n")
        rows = []
        for r in data["payments"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_num(r[4])}, {escape_sql_str(r[5])}, {escape_sql_str(r[6])}, {escape_sql_str(r[7])}, {escape_sql_str(r[8])}, {escape_sql_str(r[9])}, {escape_sql_str(r[10])}, {format_ts(r[11])}, {format_ts(r[12])}, {format_ts(r[13])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 2. PAYMENT_EVENTS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 2. PAYMENT_EVENTS ({len(data['payment_events'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO payment_events (event_id, payment_id, event_type, from_status, to_status, event_source, event_payload, event_timestamp) VALUES\n")
        rows = []
        for r in data["payment_events"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {escape_sql_str(r[5])}, {escape_sql_json(r[6])}, {format_ts(r[7])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 3. BANK_RECORDS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 3. BANK_RECORDS ({len(data['bank_records'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO bank_records (bank_record_id, payment_id, bank_name, bank_reference_number, utr_number, account_last4, bank_status, debited_amount, currency, response_code, response_message, network_latency_ms, bank_timestamp, raw_payload, created_at) VALUES\n")
        rows = []
        for r in data["bank_records"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {escape_sql_str(r[5])}, {escape_sql_str(r[6])}, {escape_sql_num(r[7])}, {escape_sql_str(r[8])}, {escape_sql_str(r[9])}, {escape_sql_str(r[10])}, {r[11] if r[11] is not None else 'NULL'}, {format_ts(r[12])}, {escape_sql_json(r[13])}, {format_ts(r[14])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 4. GATEWAY_RECORDS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 4. GATEWAY_RECORDS ({len(data['gateway_records'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO gateway_records (gateway_record_id, payment_id, gateway_name, gateway_transaction_id, gateway_order_id, auth_status, capture_status, gateway_status, authorized_amount, captured_amount, fee, tax, error_code, error_description, processing_latency_ms, gateway_timestamp, raw_payload, created_at) VALUES\n")
        rows = []
        for r in data["gateway_records"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {escape_sql_str(r[5])}, {escape_sql_str(r[6])}, {escape_sql_str(r[7])}, {escape_sql_num(r[8])}, {escape_sql_num(r[9])}, {escape_sql_num(r[10])}, {escape_sql_num(r[11])}, {escape_sql_str(r[12])}, {escape_sql_str(r[13])}, {r[14] if r[14] is not None else 'NULL'}, {format_ts(r[15])}, {escape_sql_json(r[16])}, {format_ts(r[17])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 5. MERCHANT_ORDER_RECORDS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 5. MERCHANT_ORDER_RECORDS ({len(data['merchant_order_records'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO merchant_order_records (merchant_order_record_id, payment_id, merchant_id, merchant_order_id, order_status, fulfillment_status, expected_amount, currency, cancellation_reason, customer_notes, merchant_updated_at, created_at) VALUES\n")
        rows = []
        for r in data["merchant_order_records"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {escape_sql_str(r[5])}, {escape_sql_num(r[6])}, {escape_sql_str(r[7])}, {escape_sql_str(r[8])}, {escape_sql_str(r[9])}, {format_ts(r[10])}, {format_ts(r[11])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 6. WEBHOOK_RECORDS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 6. WEBHOOK_RECORDS ({len(data['webhook_records'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO webhook_records (webhook_id, payment_id, merchant_id, event_name, target_url, attempt_count, max_attempts, delivery_status, http_status_code, latency_ms, request_payload_hash, request_payload, response_body, first_attempt_at, last_attempt_at, next_retry_at, created_at) VALUES\n")
        rows = []
        for r in data["webhook_records"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {r[5]}, {r[6]}, {escape_sql_str(r[7])}, {r[8] if r[8] is not None else 'NULL'}, {r[9] if r[9] is not None else 'NULL'}, {escape_sql_str(r[10])}, {escape_sql_json(r[11])}, {escape_sql_str(r[12])}, {format_ts(r[13])}, {format_ts(r[14])}, {format_ts(r[15])}, {format_ts(r[16])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 7. SETTLEMENT_RECORDS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 7. SETTLEMENT_RECORDS ({len(data['settlement_records'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO settlement_records (settlement_id, payment_id, merchant_id, batch_id, gross_amount, fee_deducted, tax_deducted, net_settled_amount, currency, settlement_status, settlement_utr, settlement_bank_account, settled_at, created_at, updated_at) VALUES\n")
        rows = []
        for r in data["settlement_records"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_num(r[4])}, {escape_sql_num(r[5])}, {escape_sql_num(r[6])}, {escape_sql_num(r[7])}, {escape_sql_str(r[8])}, {escape_sql_str(r[9])}, {escape_sql_str(r[10])}, {escape_sql_str(r[11])}, {format_ts(r[12])}, {format_ts(r[13])}, {format_ts(r[14])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 8. REFUND_RECORDS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 8. REFUND_RECORDS ({len(data['refund_records'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO refund_records (refund_id, payment_id, merchant_id, gateway_refund_id, refund_arn, amount, currency, refund_reason, refund_speed, refund_status, bank_reversal_status, initiated_at, processed_at, raw_response) VALUES\n")
        rows = []
        for r in data["refund_records"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {escape_sql_num(r[5])}, {escape_sql_str(r[6])}, {escape_sql_str(r[7])}, {escape_sql_str(r[8])}, {escape_sql_str(r[9])}, {escape_sql_str(r[10])}, {format_ts(r[11])}, {format_ts(r[12])}, {escape_sql_json(r[13])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 9. INCIDENT_CASES
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 9. INCIDENT_CASES ({len(data['incident_cases'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO incident_cases (incident_id, payment_id, incident_type, severity, case_status, trigger_source, assigned_investigator, title, description, opened_at, resolved_at, updated_at) VALUES\n")
        rows = []
        for r in data["incident_cases"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {escape_sql_str(r[5])}, {escape_sql_str(r[6])}, {escape_sql_str(r[7])}, {escape_sql_str(r[8])}, {format_ts(r[9])}, {format_ts(r[10])}, {format_ts(r[11])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 10. INVESTIGATION_EVIDENCE
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 10. INVESTIGATION_EVIDENCE ({len(data['investigation_evidence'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO investigation_evidence (evidence_id, incident_id, payment_id, evidence_source, evidence_type, file_path, raw_content, payload_checksum, captured_at) VALUES\n")
        rows = []
        for r in data["investigation_evidence"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {escape_sql_str(r[5])}, {escape_sql_str(r[6])}, {escape_sql_str(r[7])}, {format_ts(r[8])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 11. ML_ASSESSMENTS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 11. ML_ASSESSMENTS ({len(data['ml_assessments'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO ml_assessments (assessment_id, incident_id, payment_id, model_version, predicted_root_cause, anomaly_score, confidence_score, suggested_action, feature_snapshot, model_explanation, assessed_at) VALUES\n")
        rows = []
        for r in data["ml_assessments"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {r[5]:.4f}, {r[6]:.4f}, {escape_sql_str(r[7])}, {escape_sql_json(r[8])}, {escape_sql_json(r[9])}, {format_ts(r[10])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 12. RESOLUTIONS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 12. RESOLUTIONS ({len(data['resolutions'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO resolutions (resolution_id, incident_id, payment_id, action_taken, resolution_type, resolved_by, resolution_notes, financial_impact_amount, liability_party, resolved_at) VALUES\n")
        rows = []
        for r in data["resolutions"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {escape_sql_str(r[5])}, {escape_sql_str(r[6])}, {escape_sql_num(r[7])}, {escape_sql_str(r[8])}, {format_ts(r[9])})")
        f.write(",\n".join(rows) + ";\n\n")

        # 13. AUDIT_EVENTS
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write(f"-- 13. AUDIT_EVENTS ({len(data['audit_events'])} rows)\n")
        f.write("-- -----------------------------------------------------------------------------\n")
        f.write("INSERT INTO audit_events (audit_id, entity_name, entity_id, action, actor_type, actor_id, previous_state, new_state, ip_address, created_at) VALUES\n")
        rows = []
        for r in data["audit_events"]:
            rows.append(f"({escape_sql_str(r[0])}, {escape_sql_str(r[1])}, {escape_sql_str(r[2])}, {escape_sql_str(r[3])}, {escape_sql_str(r[4])}, {escape_sql_str(r[5])}, {escape_sql_json(r[6])}, {escape_sql_json(r[7])}, {escape_sql_str(r[8])}, {format_ts(r[9])})")
        f.write(",\n".join(rows) + ";\n\n")

        f.write("SET FOREIGN_KEY_CHECKS = 1;\n")

if __name__ == "__main__":
    print("Generating payment dataset...")
    data = generate_dataset()
    print(f"Generated {len(data['payments'])} payments.")
    print(f"Generated {len(data['payment_events'])} payment events.")
    print(f"Generated {len(data['bank_records'])} bank records.")
    print(f"Generated {len(data['gateway_records'])} gateway records.")
    print(f"Generated {len(data['merchant_order_records'])} merchant order records.")
    print(f"Generated {len(data['webhook_records'])} webhook records.")
    print(f"Generated {len(data['settlement_records'])} settlement records.")
    print(f"Generated {len(data['refund_records'])} refund records.")
    print(f"Generated {len(data['incident_cases'])} incident cases.")
    print(f"Generated {len(data['investigation_evidence'])} evidence records.")
    print(f"Generated {len(data['ml_assessments'])} ML assessments.")
    print(f"Generated {len(data['resolutions'])} resolutions.")
    print(f"Generated {len(data['audit_events'])} audit events.")
    
    write_seed_sql(data, "seed.sql")
    print("Successfully wrote seed.sql!")
