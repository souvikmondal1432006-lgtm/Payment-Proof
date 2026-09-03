# Payment Proof — ML Incident Classification Engine (Phase 3)

The **Payment Incident ML Engine** is a high-performance, independent Python machine learning microservice for **Payment Proof**. It analyzes multi-party payment telemetry across Bank switches, Payment Gateways, Merchant Order Management Systems (OMS), and Webhook delivery engines to classify anomalous payment incidents, compute confidence scores, extract top contributing signals, and enforce safety invariants.

---

## 🎯 Incident Classes (9 Target Classes)

1. `NORMAL`: End-to-end synchronized success or benign network delay.
2. `DELAYED_CONFIRMATION`: High bank switch latency (>30s) with eventual capture.
3. `BANK_DEBIT_GATEWAY_FAILURE`: Customer debited by bank, but gateway failed/timed out (Ghost Debit).
4. `MISSING_WEBHOOK`: Gateway successfully captured funds, but webhook delivery dropped/failed.
5. `DUPLICATE_PAYMENT`: Multiple distinct bank debits for an identical merchant order.
6. `REFUND_UNCERTAINTY`: Gateway initiated refund, but bank reversal settlement is unacknowledged.
7. `SETTLEMENT_MISMATCH`: Captured payment has a fee/net amount payout discrepancy.
8. `ORDER_PAYMENT_CONFLICT`: Merchant inventory reservation expired before 3DS callback returned.
9. `UNRESOLVED`: Contested state, chargeback, or severely conflicting multi-party telemetry.

---

## 🏗️ Architecture & Strict Invariants

```
┌─────────────────────────────────────────────────────────────┐
│                 Java Spring Boot Backend                    │
│            (Authoritative Business Workflow Engine)         │
└──────────────────────────────┬──────────────────────────────┘
                               │ POST /api/classify
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Payment Proof ML Service                    │
│                 (FastAPI + scikit-learn)                    │
│                                                             │
│  • REST Inference Endpoint (/api/classify)                  │
│  • Real-Time Feature Preprocessing (DomainSignalExtractor)  │
│  • Random Forest Classifier Artifact (120 Trees)            │
│  • Signal Explainability Engine                             │
│  • Prohibit-Retry Safety Invariant Engine                   │
└─────────────────────────────────────────────────────────────┘
```

### Critical Operational Rules:
- **No Direct Database Access**: The ML service never connects to MySQL.
- **Advisory Role**: The ML service is purely advisory; it provides evidence and recommendations to Java.
- **Zero Target Leakage**: Feature engineering strictly relies only on observable telemetry available at time of investigation.
- **Prohibit-Retry Invariant**: If customer funds were debited during an active incident or ambiguous failure, retry is strictly prohibited to prevent duplicate debits.

---

## 📊 Model Evaluation & Benchmark Results

Evaluated on a **held-out test split (1,400 samples, 20% stratified)**:

| Metric | Baseline (Logistic Regression) | Chosen (Random Forest Classifier) |
|---|---|---|
| **Overall Accuracy** | **99.71%** | **100.00%** |
| **Macro Precision** | **99.93%** | **100.00%** |
| **Macro Recall** | **99.55%** | **100.00%** |
| **Macro F1-Score** | **99.73%** | **100.00%** |
| **Training Seed** | Fixed `random_state=42` | Fixed `random_state=42` |
| **Model Size** | ~12 KB | ~480 KB |
| **Inference Latency** | ~0.8 ms | ~2.1 ms |

### False Positive Rate (FPR) per Class:
- `BANK_DEBIT_GATEWAY_FAILURE`: **0.0000** (0 false alarms / 1,288 negatives)
- `MISSING_WEBHOOK`: **0.0000** (0 false alarms / 1,288 negatives)
- `DUPLICATE_PAYMENT`: **0.0000** (0 false alarms / 1,302 negatives)
- `NORMAL`: **0.0000** (0 false alarms / 784 negatives)

---

## 💰 Financial Business Cost Analysis

Misclassification errors in financial payment systems have asymmetric business costs:

| Error Category | Cost Penalty | Baseline Error Count | Random Forest Error Count |
|---|---|---|---|
| **Normal False Alarms** (Triage overhead) | ₹150 / case | 0 | 0 |
| **Missed Ghost Debits** (Ombudsman/dispute fee) | ₹1,200 / case | 0 | 0 |
| **False Ghost Debit Refunds** (Merchant loss) | ₹2,500 / case | 0 | 0 |
| **Missed Missing Webhooks** (Customer churn) | ₹500 / case | 0 | 0 |
| **False Duplicate Reversals** (Order friction) | ₹400 / case | 0 | 0 |
| **Other Misclassifications** (Triage routing delay) | ₹300 / case | 4 | 0 |
| **Total Expected Error Cost** | — | **₹1,200.00** | **₹0.00** |
| **Financial Risk Exposure Reduction** | — | — | **₹1,200.00 (-100.0%)** |

---

## 🚀 Quick Start & Usage

### 1. Setup Virtual Environment & Dependencies
```bash
cd ml-service
python -m venv .venv
.venv/Scripts/pip install -r requirements.txt  # Windows
# source .venv/bin/activate && pip install -r requirements.txt  # Linux/macOS
```

### 2. Train Models & Run Evaluation
```bash
.venv/Scripts/python -m training.train_pipeline
.venv/Scripts/python -m evaluation.evaluate
.venv/Scripts/python -m evaluation.business_cost
```

### 3. Run Unit & Integration Tests
```bash
.venv/Scripts/pytest -v
```

### 4. Start the FastAPI Server
```bash
.venv/Scripts/uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

---

## 📡 API Endpoints

### `POST /api/classify`
Classify payment incident telemetry.

**Request:**
```bash
curl -X POST "http://localhost:8000/api/classify" \
  -H "Content-Type: application/json" \
  -d '{
    "payment_id": "pay_000024",
    "amount": 4999.00,
    "payment_method": "UPI",
    "bank": "HDFC",
    "gateway": "RAZORPAY",
    "bank_status": "DEBITED",
    "gateway_status": "FAILED",
    "auth_status": "TIMEOUT",
    "capture_status": "FAILED",
    "merchant_order_status": "CANCELLED",
    "merchant_fulfillment_status": "CANCELLED",
    "webhook_status": "FAILED",
    "bank_latency_ms": 420,
    "gateway_latency_ms": 65000
  }'
```

**Response:**
```json
{
  "payment_id": "pay_000024",
  "classification": "BANK_DEBIT_GATEWAY_FAILURE",
  "confidence": 0.9924,
  "anomaly_score": 0.9850,
  "model_version": "incident-classifier-v1.0.0-rf",
  "top_contributing_signals": [
    {
      "signal_name": "bank_status_debited",
      "signal_value": "DEBITED",
      "importance_weight": 0.45,
      "interpretation": "Bank confirmed customer funds were successfully debited from account."
    },
    {
      "signal_name": "gateway_status_failure",
      "signal_value": "FAILED",
      "importance_weight": 0.38,
      "interpretation": "Gateway aggregator reported transaction timeout or failure."
    },
    {
      "signal_name": "merchant_order_status",
      "signal_value": "CANCELLED",
      "importance_weight": 0.17,
      "interpretation": "Merchant OMS marked cart session as CANCELLED due to lack of immediate confirmation."
    }
  ],
  "class_probabilities": {
    "BANK_DEBIT_GATEWAY_FAILURE": 0.9924,
    "NORMAL": 0.0,
    "DELAYED_CONFIRMATION": 0.0012,
    "MISSING_WEBHOOK": 0.0,
    "DUPLICATE_PAYMENT": 0.0024,
    "REFUND_UNCERTAINTY": 0.0,
    "SETTLEMENT_MISMATCH": 0.0,
    "ORDER_PAYMENT_CONFLICT": 0.0020,
    "UNRESOLVED": 0.0020
  },
  "is_retry_prohibited_recommendation": true,
  "recommended_action": "AUTO_REFUND_CUSTOMER"
}
```

### `GET /api/health`
Health check endpoint.
```bash
curl -X GET "http://localhost:8000/api/health"
```

### `GET /api/model/info`
Returns model metadata, classes, and feature lists.
```bash
curl -X GET "http://localhost:8000/api/model/info"
```
