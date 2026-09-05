# Payment Proof 🛡️
### Multi-Party Payment Discrepancy & AI Forensic Investigation Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.11+-blue.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110+-teal.svg)](https://fastapi.tiangolo.com/)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5-purple.svg)](https://vitejs.dev/)
[![Scikit-Learn](https://img.shields.io/badge/scikit--learn-1.4+-orange.svg)](https://scikit-learn.org/)
[![E2E Tests](https://img.shields.io/badge/E2E%20Tests-23%2F23%20Passed-success.svg)](test_e2e_all_scenarios.py)

---

## 💡 The Real-World Problem: The "Ghost Debit" Nightmare

Have you ever ordered food on Swiggy or booked movie tickets on BookMyShow using UPI, watched your banking app show **"₹4,500 Debited (UTR: 984102947101)"**, only to look at the merchant screen and see **"Payment Failed / Order Cancelled"**?

When you call customer support:
* **The Bank says:** *"The money left your account successfully. Contact the merchant."*
* **The Payment Gateway says:** *"The transaction timed out. No funds were captured."*
* **The Merchant says:** *"We never received confirmation within the session window, so we released your cart."*
* **The Webhook Service:** Dropped the notification silently after 3 failed retries due to an HTTP 504 error.

### Why does this happen?
In modern payment networks (UPI, NetBanking, Card switches), a transaction is **never a single atomic database update**. It is an asynchronous, distributed dance across at least **five independent institutions**:
1. **Core Banking System (CBS) / NPCI Switch** (Customer's bank)
2. **Acquiring Bank / Switch** (Merchant's bank)
3. **Payment Aggregator / Gateway PSP** (Razorpay, Cashfree, PayU)
4. **Merchant Order Management System (OMS)** (Swiggy, Zomato, Flipkart)
5. **Webhook Dispatch Brokers** (Asynchronous event queues)

When a 65-second upstream network timeout occurs between the gateway and the merchant, **each system records a completely different version of reality**.

Today, operations and risk teams spend hours manually cross-referencing bank UTRs, gateway logs, merchant order IDs, and webhook traces across spreadsheets to decide:
* *Do we refund the customer?*
* *Do we fulfill the order?*
* *Did the customer accidentally get double-debited by hitting retry?*

**Payment Proof** automates this entire forensic investigation process. It pulls telemetry from all parties, detects contradictions, uses a trained Random Forest model to identify the root cause, enforces strict Java safety invariants to protect customer money, explains the findings in plain English via Gemini AI, and locks every decision in an unbroken SHA-256 cryptographic audit ledger.

---

## 🏛️ System Architecture & Separation of Concerns

Payment Proof is built with a strict separation of concerns: **AI and Machine Learning advise, but deterministic Java code holds ultimate authority over financial state.**

```
                                  USER BROWSER / OPERATOR
                                             │
                                             ▼
                       ┌───────────────────────────────────────────┐
                       │       React 18 Forensic Workstation       │
                       │     (Dark-mode, Millisecond Timelines,    │
                       │    Evidence Matrix, Cryptographic Audit)  │
                       └─────────────────────┬─────────────────────┘
                                             │ HTTP /api
                                             ▼
 ┌────────────────────────────────────────────────────────────────────────────────────────┐
 │                      JAVA 21 SPRING BOOT AUTHORITATIVE CORE (Port 8080)               │
 │                                                                                        │
 │  ┌─────────────────────────┐  ┌───────────────────────────┐  ┌──────────────────────┐  │
 │  │ Incident & Case Manager │  │ Multi-Party Truth Engine  │  │ Resolution Authority │  │
 │  └────────────┬────────────┘  └─────────────┬─────────────┘  └──────────┬───────────┘  │
 │               │                             │                           │              │
 │               │                             │     ┌─────────────────────┘              │
 │               │                             │     ▼                                    │
 │               │                 ┌────────────────────────────────┐                     │
 │               │                 │  Deterministic Safety Rules    │                     │
 │               │                 │  • Blind retry strictly locked │                     │
 │               │                 │  • Multi-party consensus check │                     │
 │               │                 │  • Zero AI financial mutation  │                     │
 │               │                 └────────────────────────────────┘                     │
 │               │                                                                        │
 │               ▼                                                                        │
 │  ┌──────────────────────────┐                      ┌────────────────────────────────┐  │
 │  │ SHA-256 Cryptographic    │                      │ Google Gemini AI Assistant     │  │
 │  │ Tamper-Proof Audit Chain │                      │ (Plain-Language Forensic RCA)  │  │
 │  └──────────────────────────┘                      └────────────────────────────────┘  │
 └─────────────────────────┬──────────────────────────────────────────────────────────────┘
                           │ POST /api/classify (Inference Only)
                           ▼
 ┌────────────────────────────────────────────────────────────────────────────────────────┐
 │                     PYTHON ML CLASSIFICATION ENGINE (FastAPI, Port 8000)               │
 │                                                                                        │
 │  • Real-time Feature Extraction (DomainSignalExtractor)                                │
 │  • 120-Tree Random Forest Classifier (Trained on 7,000+ real discrepancy records)      │
 │  • 9 Target Incident Classes (Ghost Debits, Dropped Webhooks, Duplicate Charges, etc.) │
 │  • Confidence Scoring & Anomaly Detection                                              │
 │  • ZERO database access (Stateless inference microservice)                             │
 └────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔒 The 3 Inviolable Safety Invariants

In payment systems, buggy AI or reckless automation can result in massive financial loss or duplicate customer charges. Payment Proof enforces three non-negotiable architectural invariants:

### 1. Invariant 1: ML and GenAI Are Strictly Advisory
The Python ML service and Gemini AI produce classifications, confidence scores, and natural-language explanations. **They are forbidden from executing refunds, altering account balances, or resolving cases.** All state transitions must be authorized by a human operator or deterministic Java business rules.

### 2. Invariant 2: The Active Debit Money Lock
If a bank switch reports a successful debit (with an active UTR number), **checkout retry is strictly prohibited.**
* *Why?* If an app blindly presents a *"Retry Payment"* button when the user's account has already been debited, the customer will be double-charged. The system automatically locks retry and routes the case to instant auto-refund or merchant re-fulfillment.

### 3. Invariant 3: Tamper-Proof Cryptographic Audit Ledger
Every state transition, rule trigger, ML evaluation, and operator action is recorded in a sequential, SHA-256 linked chain:
$$\text{CurrentHash} = \text{SHA-256}(\text{Seq} + \text{PrevHash} + \text{Actor} + \text{Action} + \text{Payload} + \text{Timestamp})$$
Any retroactive tampering or deletion of a database record immediately breaks the mathematical chain verification, providing verifiable legal proof for banking ombudsmen and dispute audits.

---

## 🔍 The Hero Incident Walkthrough (`inc_test_001`)

To understand Payment Proof in action, consider the seeded Hero Incident:
* **Customer**: Aarav Sharma
* **Merchant**: Swiggy (`merch_swiggy_ind`)
* **Order ID**: `ORD-2026-TEST01`
* **Amount**: ₹4,500.00 via PhonePe UPI
* **Bank UTR**: `UTR984102947101`

```
T + 00:00.000  [CLIENT_SDK]     Customer Aarav Sharma initiates ₹4,500 checkout via PhonePe UPI.
T + 00:00.420  [BANK_SWITCH]    HDFC Bank switch approves debit in 420ms. UTR: UTR984102947101 generated.
T + 01:05.000  [GATEWAY_ENGINE] Razorpay gateway switch times out after 65,000ms. Auth: FAILED, Capture: NONE.
T + 01:06.000  [WEBHOOK_BROKER] Webhook to Swiggy endpoint fails with HTTP 504 Gateway Timeout (3 retries).
T + 01:10.000  [MERCHANT_OMS]   Swiggy cart reservation expires. Order CANCELLED (SESSION_TIMEOUT_NO_PROOF).
```

### What Payment Proof Does:
1. **Multi-Party Consensus Matrix**: Shows all 4 systems side-by-side. Bank confirms ₹4,500 debited; Gateway confirms 65s timeout; Merchant confirms order cancelled; Webhook confirms dropped.
2. **Contradiction Detection**: Flags the severe conflict: *Bank Debited vs Gateway Failed*.
3. **ML Classification**: Random Forest identifies the pattern as `BANK_DEBIT_GATEWAY_FAILURE` with **99.2% confidence**.
4. **Safety Lock Activated**: Detects active bank debit. Locks checkout retry. Flags ₹4,500 at risk.
5. **Gemini Forensic Narrative**: Generates an executive briefing explaining what happened and advising an immediate auto-refund.
6. **Action Execution**: One-click remediation issues a source refund of ₹4,500 to Aarav Sharma's bank account.
7. **Audit Chain Update**: Appends event `#1146` to the cryptographic chain.

---

## 🎯 The 9 Discrepancy Patterns Handled

| Pattern Code | Real-World Scenario | Automated Resolution |
| :--- | :--- | :--- |
| `BANK_DEBIT_GATEWAY_FAILURE` | **Ghost Debit**: Bank debited, gateway timed out, order cancelled. | Auto-refund customer; block retry. |
| `MISSING_WEBHOOK` | **Dropped Notification**: Gateway captured funds, but merchant webhook dropped. | Replay webhook asynchronously to unblock fulfillment. |
| `DELAYED_CONFIRMATION` | **High Latency Switch**: Bank took >60s to respond; captured late. | Re-query banking switch; update merchant order. |
| `DUPLICATE_PAYMENT` | **Double Charge**: Customer tapped retry twice on slow network; 2 debits for 1 order. | Detect duplicate UTR; auto-refund second charge. |
| `REFUND_UNCERTAINTY` | **Hanging Refund**: Gateway initiated refund, but bank clearing switch delayed ARN. | Flag for automated banking switch reconciliation. |
| `SETTLEMENT_MISMATCH` | **MDR Variance**: Gateway captured gross amount does not reconcile with net merchant batch payout. | Recalculate MDR fee/tax; post adjustment entry. |
| `ORDER_PAYMENT_CONFLICT` | **Cart Expiry**: Merchant timer released inventory 5s before 3DS callback returned. | Auto-refund customer; alert merchant stock inventory. |
| `NORMAL` | **Happy Path**: Bank, Gateway, Merchant, and Webhook all agree. | Verified synchronized; zero operator intervention. |
| `UNRESOLVED` | **Ambiguous Dispute**: Contested chargeback with conflicting proof of delivery. | Escalate to Tier-3 human investigator. |

---

## 🚀 Quick Start (Run Locally in 3 Minutes)

Payment Proof runs out-of-the-box on Windows, Linux, and macOS.

### Prerequisites
* **Java 21** (`java -version`)
* **Node.js 18+** (`node -v`)
* **Python 3.11+** (`python --version`)
* *(Optional)* MySQL 8.0 (The backend defaults to an automated zero-config in-memory H2 database pre-seeded with 200 real incidents if MySQL is not running).

---

### Step 1: Start the Python ML Service (Port 8000)
```bash
# From repository root
cd ml-service

# Create and activate virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .\.venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Start FastAPI server
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
*Health Check*: Open `http://localhost:8000/health` $\rightarrow$ `{"status": "healthy", "model_loaded": true}`.

---

### Step 2: Start the Java Spring Boot Backend (Port 8080)
```bash
# Open a new terminal from repository root
# Using the bundled Maven wrapper
.\.maven\bin\mvn -f backend/pom.xml spring-boot:run
# Or if you have maven installed globally:
mvn -f backend/pom.xml spring-boot:run
```
*Health Check*: Open `http://localhost:8080/api/health` $\rightarrow$ `{"status": "UP", "service": "PAYMENT_PROOF_GATEWAY"}`.

---

### Step 3: Start the React Frontend (Port 3000)
```bash
# Open a new terminal from repository root
cd frontend

# Install dependencies
npm install

# Start Vite dev server
npm run dev
```
*Open in Browser*: **`http://localhost:3000`**

---

## 🧪 Comprehensive Testing & Verification

Payment Proof includes an automated test suite verifying both component-level logic and multi-party distributed invariants:

### 1. Run Complete 23-Scenario E2E Validation Suite
```bash
python test_e2e_all_scenarios.py
```
This script tests:
* All 8 core discrepancy scenarios
* ML offline fallback & timeout resilience
* Gemini absence fallback & deterministic templates
* Safety Invariant 1 (Rejecting AI-authorized resolutions)
* Safety Invariant 2 (Rejecting unauthorized Gemini resolutions)
* Safety Invariant 3 (Active Debit Money Lock)
* Cryptographic SHA-256 chain integrity verification

### 2. Run Java Backend Tests
```bash
mvn -f backend/pom.xml test
```
Runs unit and integration tests for `InvestigationService`, `TimelineService`, `AuditChainService`, and `ResolutionService`.

### 3. Run Python ML Service Tests
```bash
cd ml-service
pytest
```
Validates model accuracy, feature extractor signal integrity, and inference latency benchmarks.

---

## 📁 Repository Structure

```
payment-proof/
├── backend/                               # Java 21 Spring Boot Service (Port 8080)
│   ├── src/main/java/com/paymentproof/
│   │   ├── client/ml/                     # WebClient for ML Service & Gemini API
│   │   ├── config/                        # Security, CORS, and DataSeeder
│   │   ├── controller/                    # REST APIs (/incidents, /payments, /audit)
│   │   ├── dto/                           # Data Transfer Objects
│   │   ├── entity/                        # JPA Entities (Payments, Telemetry, Audit)
│   │   ├── repository/                    # Spring Data Repositories
│   │   └── service/                       # Forensic Investigation & Safety Invariants
│   └── src/main/resources/
│       ├── application.yml                # Configuration profiles
│       └── seed-h2.sql                    # Pre-populated discrepancy dataset
├── ml-service/                            # Python FastAPI ML Microservice (Port 8000)
│   ├── app/
│   │   ├── api/v1/router.py               # REST classification endpoint
│   │   ├── core/config.py                 # Service configuration
│   │   ├── schemas/incident.py            # Pydantic telemetry input schemas
│   │   └── services/
│   │       ├── classifier.py              # Random Forest model loader & inference
│   │       └── feature_extractor.py       # Domain feature engineering
│   ├── artifacts/                         # Serialized scikit-learn model (.joblib)
│   └── requirements.txt                   # FastAPI, scikit-learn, joblib, uvicorn
├── frontend/                              # React 18 + Vite Forensic Workstation (Port 3000)
│   ├── src/
│   │   ├── components/
│   │   │   ├── CommandCenterView.jsx      # High-level operations & triage KPI stream
│   │   │   ├── PaymentInvestigationView.jsx # Multi-party evidence & contradiction cards
│   │   │   ├── TimelineView.jsx           # Millisecond chronological event trajectory
│   │   │   ├── AiConclusionView.jsx       # Gemini explanation & ML diagnostics
│   │   │   ├── ResolutionCenterView.jsx   # One-click remediation & safety locks
│   │   │   └── AuditLedgerView.jsx        # SHA-256 cryptographic chain inspector
│   │   ├── services/api.js                # Authoritative API client & presentation store
│   │   └── App.jsx                        # Main state machine
│   ├── package.json
│   └── vite.config.js                     # Proxy configuration
├── database/                              # Relational Database Documentation & SQL
│   ├── schema.sql                         # MySQL production schema (13 entities)
│   ├── seed.sql                           # Comprehensive incident seed data
│   └── generate_seed.py                   # Automated synthetic telemetry generator
├── test_e2e_all_scenarios.py              # 23-Scenario Automated Integration Suite
├── vercel.json                            # Cloud deployment configuration
└── package.json                           # Root monorepo workspace scripts
```

---

## 🎬 Guided Forensic Investigation Workflow

To trace a complete end-to-end multi-party investigation:
1. **Command Center**: Navigate to the triage dashboard. Review operations streams across merchants (Swiggy, Zomato, BookMyShow) and observe the active discrepancy queue.
2. **Select Incident Case (`inc_test_001`)**: Open the Swiggy ₹4,500 Ghost Debit incident.
3. **Execute Forensic Analysis**: Click *"Run Forensic Investigation"*. Review the **Contradictions Banner** highlighting the state divergence between Bank success and Gateway timeout.
4. **Multi-Party Consensus Matrix**: Compare all 4 independent sources: Bank switch (SUCCESS, UTR generated), Gateway (FAILED, 65s timeout), Merchant OMS (CANCELLED), Webhook (DROPPED, HTTP 504).
5. **Timeline Chronology**: Inspect the **Timeline** tab. Trace the millisecond sequence from payment creation to gateway socket disconnect and webhook drop.
6. **Machine Learning & Safety Invariants**: Check the **AI Report** tab. Note the Random Forest diagnosis (`BANK_DEBIT_GATEWAY_FAILURE` with 99.2% confidence) and the deterministic Java active-debit money lock that prohibits blind retry.
7. **Executive AI Briefing**: Review the plain-language executive explanation generated by the Gemini assistant.
8. **Resolution Execution**: Open the **Resolution Center**. Authorize the automated source refund of ₹4,500.00 to the customer's bank account.
9. **Audit Verification**: Navigate to **Audit Log** and verify the cryptographic integrity of the SHA-256 chain to ensure all actions are immutably signed.

---

## 📜 License & Acknowledgements
Developed as a production-grade forensic prototype for multi-party payment reconciliation and financial dispute resolution.
Licensed under the [Apache License 2.0](LICENSE).
