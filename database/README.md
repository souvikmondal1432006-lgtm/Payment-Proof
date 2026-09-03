# Payment Proof — MySQL Database Layer (Phase 1)

Authoritative relational database schema and forensic investigation environment for **Payment Proof**, designed for multi-party payment reconciliation, discrepancy detection, and dispute resolution.

---

## 1. Architectural Philosophy

In real-world payment ecosystems (such as UPI, Card networks, and NetBanking in India), a transaction is **not a single atomic database state**. Instead, it is a distributed workflow across at least four distinct systems:
1. **Core Banking System (CBS) / NPCI / Card Switch** (e.g. HDFC, ICICI, SBI)
2. **Payment Gateway / Aggregator** (e.g. Razorpay, Cashfree, PayU, BillDesk)
3. **Merchant Order Management System (OMS)** (e.g. Flipkart, Swiggy, Zomato, Zepto)
4. **Webhook Delivery Service** (asynchronous event broker between Gateway and Merchant)

Because network partitions, socket timeouts, merchant session expirations, and dropped webhooks occur frequently, **a payment can easily have contradictory states across different systems**.

### Example Contradiction:
```
┌─────────────────────────────────────────────────────────────┐
│  Payment Intent: ₹8,500.00 / UPI (Payment ID: pay_000024)   │
├───────────────────┬─────────────────────────────────────────┤
│ System            │ Reported State                          │
├───────────────────┼─────────────────────────────────────────┤
│ 1. Bank Record    │ SUCCESS (Funds debited, UTR generated)  │
│ 2. Gateway Record │ PENDING / TIMED_OUT                     │
│ 3. Merchant OMS   │ CANCELLED (Cart session expired)        │
│ 4. Webhook Log    │ DROPPED / FAILED (HTTP 504 / unreachable)│
│ 5. Settlement     │ ON_HOLD                                 │
│ 6. Refund Record  │ PROCESSED (Auto-reversal initiated)     │
└───────────────────┴─────────────────────────────────────────┘
```

The **Payment Proof Database** models this multi-party reality naturally through **13 dedicated relational entities**.

---

## 2. The 13 Relational Entities

| # | Entity Table | Primary Key | Description & Domain Purpose |
|---|---|---|---|
| 1 | `payments` | `payment_id` | Canonical payment attempt representing the customer's intent to pay a merchant. Stores amount (`DECIMAL(12,2)`), payment method, status, client metadata, and timestamps. |
| 2 | `payment_events` | `event_id` | High-resolution lifecycle event log tracking chronological state transitions (`INITIATED`, `AUTH_REQUESTED`, `DEBIT_ACK`, `CAPTURED`, `FAILED`). |
| 3 | `bank_records` | `bank_record_id` | Core banking switch telemetry: bank name, UTR (Unique Transaction Reference), bank RRN, account last 4 digits, response codes (e.g., `00`, `U30`), and network latency. |
| 4 | `gateway_records` | `gateway_record_id` | Aggregator/Gateway processor state: auth status, capture status, fee & tax deductions (`DECIMAL(12,2)`), error codes, and processing latency. |
| 5 | `merchant_order_records` | `merchant_order_record_id` | Merchant OMS perspective: order ID, cart status (`PAID`, `CANCELLED`, `EXPIRED`), fulfillment status (`DELIVERED`, `UNFULFILLED`), and cancellation reason. |
| 6 | `webhook_records` | `webhook_id` | Webhook notification dispatch logs: target URL, attempt counts, delivery status (`DELIVERED`, `FAILED`, `DROPPED`), HTTP status codes (`200`, `504`), and payload hashes. |
| 7 | `settlement_records` | `settlement_id` | Merchant payout ledger: batch ID, gross amount, MDR fees deducted, tax deducted, net settled amount, settlement UTR, and status (`SETTLED`, `ON_HOLD`, `DISCREPANCY`). |
| 8 | `refund_records` | `refund_id` | Refund lifecycle ledger: gateway refund ID, Acquirer Reference Number (ARN), refund reason, refund speed (`INSTANT` vs `NORMAL`), and bank reversal status. |
| 9 | `incident_cases` | `incident_id` | Root dispute and contradiction cases opened by automated reconciliation or customer/merchant tickets. Captures severity, incident type, and assigned investigator. |
| 10 | `investigation_evidence` | `evidence_id` | Multi-source forensic artifacts attached to an incident: ISO-8583 payloads, JSON telemetry dumps, HTTP traces, and account statements with SHA-256 checksums. |
| 11 | `ml_assessments` | `assessment_id` | Machine learning model diagnostics: predicted root cause, anomaly score (0.0000–1.0000), confidence score, recommended action, and feature vector snapshot. |
| 12 | `resolutions` | `resolution_id` | Authoritative, binding dispute resolution records: action taken (`CUSTOMER_REFUNDED`, `MERCHANT_CREDITED`, `DUPLICATE_REVERSED`), liability party, and financial impact. |
| 13 | `audit_events` | `audit_id` | Immutable audit log capturing all system state mutations, automated rule executions, and operator overrides with actor IDs, previous/new JSON states, and timestamps. |

---

## 3. Supported Incident Scenarios in Seed Data

The database comes pre-populated with **560 payments** and over **7,400 related records** encompassing realistic distributions:

1. **Delayed Confirmation (`DELAYED_CONFIRMATION`)**
   - *Behavior*: Bank debit took 120+ seconds to respond. Gateway timed out initially but captured late upon secondary polling. Merchant fulfilled asynchronously.
2. **Bank Debit but Gateway Failure (`BANK_DEBIT_GATEWAY_FAILURE`)**
   - *Behavior*: Customer account debited (Bank = SUCCESS, UTR generated), but Gateway encountered a socket timeout (Gateway = FAILED), Merchant marked cart as CANCELLED. Auto-refunded to customer.
3. **Gateway Success but Missing Webhook (`GATEWAY_SUCCESS_MISSING_WEBHOOK`)**
   - *Behavior*: Gateway successfully captured funds, but merchant webhook timed out (HTTP 504) after 3 attempts. Merchant OMS left order unfulfilled. Operator replayed webhook.
4. **Duplicate Payment (`DUPLICATE_PAYMENT`)**
   - *Behavior*: Customer clicked retry on a slow checkout screen; two separate successful payment charges were debited for the same `order_id`. Duplicate charge was flagged and reversed.
5. **Refund Uncertainty (`REFUND_UNCERTAINTY`)**
   - *Behavior*: Merchant processed refund at Gateway, but the banking network clearing switch delayed/failed the reversal credit, leaving ARN unacknowledged.
6. **Settlement Mismatch (`SETTLEMENT_MISMATCH`)**
   - *Behavior*: Gateway captured amount minus MDR fee & tax does not match the actual net settled amount in the merchant payout batch.
7. **Merchant Cancellation Before Confirmation (`MERCHANT_CANCELLATION_BEFORE_CONFIRMATION`)**
   - *Behavior*: Merchant inventory timer expired at 5m00s and released stock; customer completed 3DS authentication at 5m12s. Bank debited, necessitating auto-refund.
8. **Conflicting Payment States (`CONFLICTING_PAYMENT_STATES`)**
   - *Behavior*: Multi-system desynchronization where Bank is DEBITED, Gateway is PENDING, Merchant is CANCELLED, and Webhook is DROPPED.
9. **Normal Successful Payment (`NORMAL_SUCCESS` & `NORMAL_PAYMENT_FALSE_ALARM`)**
   - *Behavior*: Standard synchronized happy path across all 4 systems (Bank=SUCCESS, Gateway=SUCCESS, Merchant=PAID/DELIVERED, Webhook=DELIVERED, Settlement=SETTLED).
10. **Genuinely Unresolved Case (`GENUINELY_UNRESOLVED_CASE`)**
    - *Behavior*: Complex open dispute involving contested chargeback claims, conflicting proof of delivery, and pending manual issuer bank escalation.

---

## 4. Setup & Installation

### Option 1: MySQL CLI
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS payment_proof CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p payment_proof < schema.sql
mysql -u root -p payment_proof < seed.sql
```

### Option 2: Regenerating Seed Data
If you wish to re-generate the seed data with different distributions or seeds:
```bash
python generate_seed.py
```

### Option 3: Verification & In-Memory Test
Run the automated test suite to verify table constraints and query execution:
```bash
python test_execution.py
```

---

## 5. Sample Investigation Queries

See [queries.sql](file:///e:/razorpay/database/queries.sql) for ready-to-run SQL queries covering:
- Ghost Debit & Orphaned Funds Triage
- Dropped Webhook Identification
- Multi-System 360-Degree Forensic Dossier
- Settlement Variance and MDR Calculation Audits
- ML Feature Matrix Export for model training
- Open Incident Backlog and Operator Queue
- Immutable Audit Trail Timeline
