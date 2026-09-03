/**
 * Payment Proof Authoritative API Client
 * Rule: Frontend communicates EXCLUSIVELY with the Java Spring Boot backend (/api).
 * Frontend MUST NOT communicate directly with MySQL or Python ML.
 */

const API_BASE = import.meta.env.VITE_API_URL || '/api';
const DEFAULT_TIMEOUT_MS = 6000;

class ApiError extends Error {
  constructor(message, humanMessage, status, technicalDetails, endpoint) {
    super(message);
    this.name = 'ApiError';
    this.humanMessage = humanMessage;
    this.status = status;
    this.technicalDetails = technicalDetails;
    this.endpoint = endpoint;
  }
}

function mapHttpToHumanError(status, defaultMsg, endpoint) {
  switch (status) {
    case 400:
      return "The payment query or investigation parameters were invalid. Please verify and try again.";
    case 404:
      return "The requested payment record or incident case could not be found. Please check the ID and retry.";
    case 408:
      return "The investigation request timed out while communicating with upstream banking networks.";
    case 422:
      return "The requested forensic action cannot be applied to this payment in its current state.";
    case 500:
      return "We couldn't complete the investigation right now. Your payment records are still safe. Try again.";
    case 502:
    case 503:
    case 504:
      return "Unable to reach the authoritative payment investigation server. The service may be starting.";
    default:
      return defaultMsg || "We couldn't complete this action right now. Your payment records remain safe. Try again.";
  }
}

async function request(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const timeoutMs = options.timeout || DEFAULT_TIMEOUT_MS;
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  const fetchOptions = {
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...options.headers
    },
    signal: controller.signal,
    ...options
  };

  try {
    const response = await fetch(url, fetchOptions);
    clearTimeout(timeoutId);

    if (!response.ok) {
      let errBody = null;
      try {
        errBody = await response.json();
      } catch {
        try {
          errBody = await response.text();
        } catch {
          errBody = response.statusText;
        }
      }

      const humanMsg = mapHttpToHumanError(
        response.status,
        errBody?.message || errBody?.error || response.statusText,
        url
      );

      throw new ApiError(
        `HTTP ${response.status} from ${endpoint}`,
        humanMsg,
        response.status,
        errBody,
        url
      );
    }

    if (response.status === 204) return {};
    return await response.json();

  } catch (err) {
    clearTimeout(timeoutId);

    if (err.name === 'ApiError') {
      throw err;
    }

    if (err.name === 'AbortError') {
      throw new ApiError(
        `Request timeout after ${timeoutMs}ms`,
        "The investigation request took longer than expected. Upstream banking switches may be experiencing elevated latency.",
        408,
        { timeoutMs, endpoint: url },
        url
      );
    }

    throw new ApiError(
      err.message || 'Network connection failure',
      "Unable to connect to the payment investigation backend server (http://localhost:8080). Check your backend service status.",
      0,
      { originalError: err.message, stack: err.stack, endpoint: url },
      url
    );
  }
}

// Built-in Forensic Mock Store for Instant Fallback
const FALLBACK_INCIDENTS = [
  {
    incidentId: 'inc_0000009',
    paymentId: 'pay_000024',
    orderId: 'ORD-2026-00024',
    merchantId: 'merch_bookmyshow',
    merchantName: 'BookMyShow India',
    amount: 8500.00,
    currency: 'INR',
    paymentMethod: 'UPI',
    customerName: 'Rahul Sharma',
    customerEmail: 'rahul.sharma@example.com',
    severity: 'CRITICAL',
    caseStatus: 'OPEN',
    incidentType: 'BANK_DEBIT_GATEWAY_FAILURE',
    title: 'Ghost Debit on BookMyShow Checkout',
    description: 'Customer debited ₹8,500 via UPI but gateway timed out and merchant auto-cancelled.',
    openedAt: '2026-08-30T10:45:10',
    moneyAtRisk: 8500.00,
    isRetryProhibited: true,
    retryReason: 'STRICT SAFETY INVARIANT: Active bank debit confirmed with UTR 414960264709. Retry strictly prohibited.',
    bank: {
      bankName: 'HDFC Bank',
      status: 'SUCCESS',
      utr: '414960264709',
      debitedAmount: 8500.00,
      latencyMs: 420,
      timestamp: '2026-08-30T10:43:20'
    },
    gateway: {
      gatewayName: 'Razorpay',
      status: 'PENDING',
      captureStatus: 'PENDING',
      authStatus: 'TIMEOUT',
      errorCode: 'GATEWAY_TIMEOUT_AFTER_65S',
      latencyMs: 65000,
      timestamp: '2026-08-30T10:44:25'
    },
    merchant: {
      merchantId: 'merch_bookmyshow',
      orderId: 'ORD-2026-00024',
      status: 'CANCELLED',
      fulfillmentStatus: 'CANCELLED',
      cancellationReason: 'SESSION_TIMEOUT_NO_PROOF',
      timestamp: '2026-08-30T10:44:30'
    },
    webhook: {
      deliveryStatus: 'DROPPED',
      httpStatusCode: 504,
      attemptCount: 3,
      targetUrl: 'https://api.bookmyshow.com/payments/webhook',
      timestamp: '2026-08-30T10:45:00'
    },
    settlement: {
      settlementStatus: 'ON_HOLD',
      grossAmount: 8500.00,
      feeDeducted: 170.00,
      taxDeducted: 30.60,
      netSettledAmount: 0.00
    },
    refund: {
      refundStatus: 'NOT_INITIATED',
      refundArn: null
    },
    aiReport: {
      whatHappened: "Your customer was charged ₹8,500 at HDFC Bank via UPI, but the payment gateway experienced an upstream timeout. Consequently, BookMyShow never received confirmation and cancelled the seat reservation.",
      whyWeThinkThis: "Evidence synthesis indicates: Bank switch reported SUCCESS (UTR: 414960264709, Latency: 420ms). Gateway reported PENDING (Latency: 65,000ms). Merchant OMS recorded CANCELLED (Reason: SESSION_TIMEOUT_NO_PROOF). Webhook delivery resulted in DROPPED (HTTP 504 after 3 attempts). Statistical ML classifier assigned 99.2% confidence to 'BANK_DEBIT_GATEWAY_FAILURE'.",
      whatIsUncertain: "Unable to confirm whether BookMyShow reallocated the cinema seats to another customer or can restore the reservation. Unable to confirm if the issuing bank scheduled an automatic T+5 clearing reversal.",
      recommendedAction: "AUTO_REFUND_CUSTOMER",
      moneyAtRisk: 8500.00,
      confidence: 0.9924,
      isRetryProhibited: true,
      retryProhibitionReason: "STRICT SAFETY INVARIANT: Active bank debit confirmed with UTR '414960264709' (₹8,500.00). Blind retry is strictly prohibited to prevent duplicate debit.",
      decisionFactors: [
        "Bank Telemetry: status=SUCCESS, utr=414960264709, debited_amount=8500.00",
        "Gateway Telemetry: status=PENDING, capture=PENDING, latency=65000ms",
        "Merchant OMS: order_status=CANCELLED, fulfillment=CANCELLED, cancellation_reason=SESSION_TIMEOUT_NO_PROOF",
        "Webhook Engine: delivery_status=DROPPED, http_code=504, attempts=3",
        "Safety Invariant: is_retry_prohibited=true",
        "ML Advisory: model=incident-classifier-v1.0.0-rf, root_cause=BANK_DEBIT_GATEWAY_FAILURE, confidence=0.9924"
      ],
      topContributingSignals: [
        { signalName: 'bank_status_debited', signalValue: 'SUCCESS', importanceWeight: 0.45, interpretation: 'Bank confirmed customer funds were successfully debited from account.' },
        { signalName: 'gateway_status_failure', signalValue: 'PENDING', importanceWeight: 0.38, interpretation: 'Gateway aggregator reported transaction timeout or failure.' },
        { signalName: 'merchant_order_status', signalValue: 'CANCELLED', importanceWeight: 0.17, interpretation: 'Merchant OMS marked cart session as CANCELLED.' }
      ],
      classProbabilities: {
        BANK_DEBIT_GATEWAY_FAILURE: 0.9924,
        DELAYED_CONFIRMATION: 0.0032,
        MISSING_WEBHOOK: 0.0021,
        DUPLICATE_PAYMENT: 0.0011,
        UNRESOLVED: 0.0012
      }
    }
  },
  {
    incidentId: 'inc_0000002',
    paymentId: 'pay_000005',
    orderId: 'ORD-2026-00005',
    merchantId: 'merch_swiggy',
    merchantName: 'Swiggy Food Delivery',
    amount: 1450.00,
    currency: 'INR',
    paymentMethod: 'NETBANKING',
    customerName: 'Ananya Roy',
    customerEmail: 'ananya.roy@example.com',
    severity: 'HIGH',
    caseStatus: 'OPEN',
    incidentType: 'MISSING_WEBHOOK',
    title: 'Dropped Webhook on Food Order',
    description: 'Payment authorized and captured by gateway, but merchant webhook dropped.',
    openedAt: '2026-08-30T11:15:00',
    moneyAtRisk: 1450.00,
    isRetryProhibited: true,
    retryReason: 'Payment captured at gateway. Resend webhook rather than retrying payment.',
    bank: {
      bankName: 'ICICI Bank',
      status: 'SUCCESS',
      utr: '992811400291',
      debitedAmount: 1450.00,
      latencyMs: 310,
      timestamp: '2026-08-30T11:14:10'
    },
    gateway: {
      gatewayName: 'Cashfree',
      status: 'SUCCESS',
      captureStatus: 'CAPTURED',
      authStatus: 'SUCCESS',
      errorCode: null,
      latencyMs: 820,
      timestamp: '2026-08-30T11:14:15'
    },
    merchant: {
      merchantId: 'merch_swiggy',
      orderId: 'ORD-2026-00005',
      status: 'PENDING_PAYMENT',
      fulfillmentStatus: 'UNFULFILLED',
      cancellationReason: null,
      timestamp: '2026-08-30T11:14:00'
    },
    webhook: {
      deliveryStatus: 'DROPPED',
      httpStatusCode: 504,
      attemptCount: 3,
      targetUrl: 'https://partner.swiggy.com/payments/webhook',
      timestamp: '2026-08-30T11:14:50'
    },
    settlement: {
      settlementStatus: 'SETTLED',
      grossAmount: 1450.00,
      feeDeducted: 29.00,
      taxDeducted: 5.22,
      netSettledAmount: 1415.78
    },
    refund: {
      refundStatus: 'NOT_INITIATED',
      refundArn: null
    },
    aiReport: {
      whatHappened: "The payment of ₹1,450.00 was successfully authorized and captured by Cashfree, but the asynchronous webhook notification dropped after 3 attempts with HTTP 504. The merchant order remains unfulfilled in PENDING_PAYMENT status.",
      whyWeThinkThis: "Evidence shows Bank and Gateway are both in SUCCESS/CAPTURED state. However, the merchant order is still PENDING_PAYMENT because webhook delivery failed. Statistical ML classifier assigned 96.5% confidence to 'MISSING_WEBHOOK'.",
      whatIsUncertain: "Unable to confirm if Swiggy's backend polled the payment status out-of-band.",
      recommendedAction: "RESEND_WEBHOOK",
      moneyAtRisk: 0.00,
      confidence: 0.9650,
      isRetryProhibited: true,
      retryProhibitionReason: "Payment captured at gateway. Resend webhook rather than retrying payment.",
      decisionFactors: [
        "Bank: SUCCESS",
        "Gateway: CAPTURED",
        "Webhook: DROPPED (HTTP 504)",
        "Merchant OMS: PENDING_PAYMENT"
      ],
      topContributingSignals: [
        { signalName: 'webhook_status_dropped', signalValue: 'DROPPED', importanceWeight: 0.52, interpretation: 'Webhook delivery to merchant endpoint timed out.' },
        { signalName: 'gateway_capture_status', signalValue: 'CAPTURED', importanceWeight: 0.35, interpretation: 'Gateway successfully captured funds.' }
      ],
      classProbabilities: {
        MISSING_WEBHOOK: 0.9650,
        NORMAL: 0.0210,
        DELAYED_CONFIRMATION: 0.0140
      }
    }
  },
  {
    incidentId: 'inc_0000003',
    paymentId: 'pay_000012',
    orderId: 'ORD-2026-00012',
    merchantId: 'merch_flipkart',
    merchantName: 'Flipkart Electronics',
    amount: 34999.00,
    currency: 'INR',
    paymentMethod: 'CREDIT_CARD',
    customerName: 'Vikram Singhania',
    customerEmail: 'vikram.singhania@example.com',
    severity: 'MEDIUM',
    caseStatus: 'OPEN',
    incidentType: 'DELAYED_CONFIRMATION',
    title: 'Extreme 3DS Latency on Laptop Purchase',
    description: 'Bank 3DS switch latency of 48.5 seconds caused client checkout timeout.',
    openedAt: '2026-08-30T11:40:00',
    moneyAtRisk: 0.00,
    isRetryProhibited: true,
    retryReason: 'Payment captured successfully after delayed clearance.',
    bank: {
      bankName: 'State Bank of India',
      status: 'SUCCESS',
      utr: 'SBI88192019283',
      debitedAmount: 34999.00,
      latencyMs: 48500,
      timestamp: '2026-08-30T11:38:20'
    },
    gateway: {
      gatewayName: 'Razorpay',
      status: 'SUCCESS',
      captureStatus: 'CAPTURED',
      authStatus: 'SUCCESS',
      errorCode: null,
      latencyMs: 49200,
      timestamp: '2026-08-30T11:39:10'
    },
    merchant: {
      merchantId: 'merch_flipkart',
      orderId: 'ORD-2026-00012',
      status: 'PAID',
      fulfillmentStatus: 'PROCESSING',
      cancellationReason: null,
      timestamp: '2026-08-30T11:39:30'
    },
    webhook: {
      deliveryStatus: 'DELIVERED',
      httpStatusCode: 200,
      attemptCount: 1,
      targetUrl: 'https://payments.flipkart.com/events',
      timestamp: '2026-08-30T11:39:35'
    },
    settlement: {
      settlementStatus: 'SETTLED',
      grossAmount: 34999.00,
      feeDeducted: 699.98,
      taxDeducted: 126.00,
      netSettledAmount: 34173.02
    },
    refund: {
      refundStatus: 'NOT_INITIATED',
      refundArn: null
    },
    aiReport: {
      whatHappened: "The core banking switch experienced extreme latency of 48.5s during 3DS processing. Although the transaction was eventually captured, the confirmation arrived after the checkout session displayed a pending screen.",
      whyWeThinkThis: "All provider telemetries reached eventual consistency in SUCCESS/PAID state, but Bank latency was 48,500ms.",
      whatIsUncertain: "No critical uncertainties; transaction is fully settled.",
      recommendedAction: "NO_ACTION_REQUIRED",
      moneyAtRisk: 0.00,
      confidence: 0.9810,
      isRetryProhibited: true,
      retryProhibitionReason: "Payment captured successfully.",
      decisionFactors: [
        "Bank: SUCCESS (latency 48.5s)",
        "Gateway: CAPTURED",
        "Merchant OMS: PAID",
        "Webhook: DELIVERED (200 OK)"
      ],
      topContributingSignals: [
        { signalName: 'bank_latency_ms', signalValue: '48500', importanceWeight: 0.61, interpretation: 'High 3DS authorization latency.' }
      ],
      classProbabilities: {
        DELAYED_CONFIRMATION: 0.9810,
        NORMAL: 0.0150
      }
    }
  },
  {
    incidentId: 'inc_0000004',
    paymentId: 'pay_000018',
    orderId: 'ORD-2026-00018',
    merchantId: 'merch_zomato',
    merchantName: 'Zomato Dining',
    amount: 3200.00,
    currency: 'INR',
    paymentMethod: 'UPI',
    customerName: 'Kavita Menon',
    customerEmail: 'kavita.m@example.com',
    severity: 'CRITICAL',
    caseStatus: 'OPEN',
    incidentType: 'DUPLICATE_PAYMENT',
    title: 'Double Debit on Dining Table Booking',
    description: 'Customer retried payment after slow screen, resulting in 2 bank debits.',
    openedAt: '2026-08-30T12:00:00',
    moneyAtRisk: 3200.00,
    isRetryProhibited: true,
    retryReason: 'Multiple debits detected. Auto-refund duplicate charge.',
    bank: {
      bankName: 'Axis Bank',
      status: 'SUCCESS',
      utr: 'AXIS1122334455',
      debitedAmount: 3200.00,
      latencyMs: 510,
      timestamp: '2026-08-30T11:58:10'
    },
    gateway: {
      gatewayName: 'PayU',
      status: 'SUCCESS',
      captureStatus: 'CAPTURED',
      authStatus: 'SUCCESS',
      errorCode: null,
      latencyMs: 720,
      timestamp: '2026-08-30T11:58:15'
    },
    merchant: {
      merchantId: 'merch_zomato',
      orderId: 'ORD-2026-00018',
      status: 'PAID',
      fulfillmentStatus: 'FULFILLED',
      cancellationReason: null,
      timestamp: '2026-08-30T11:58:20'
    },
    webhook: {
      deliveryStatus: 'DELIVERED',
      httpStatusCode: 200,
      attemptCount: 1,
      targetUrl: 'https://api.zomato.com/webhook',
      timestamp: '2026-08-30T11:58:25'
    },
    settlement: {
      settlementStatus: 'SETTLED',
      grossAmount: 3200.00,
      feeDeducted: 64.00,
      taxDeducted: 11.52,
      netSettledAmount: 3124.48
    },
    refund: {
      refundStatus: 'NOT_INITIATED',
      refundArn: null
    },
    aiReport: {
      whatHappened: "Evidence indicates the customer encountered a slow screen and retried checkout, resulting in 2 distinct successful bank debits for the single merchant order reference ORD-2026-00018.",
      whyWeThinkThis: "Two distinct bank transactions with separate UTRs were mapped to the same merchant order ID.",
      whatIsUncertain: "Unable to confirm if customer noticed the duplicate debit before raising ticket.",
      recommendedAction: "AUTO_REFUND_CUSTOMER",
      moneyAtRisk: 3200.00,
      confidence: 0.9940,
      isRetryProhibited: true,
      retryProhibitionReason: "Double debit detected. Re-attempt is blocked.",
      decisionFactors: [
        "Duplicate Bank UTRs for same Order ORD-2026-00018",
        "First transaction fulfilled, second captured without order linkage"
      ],
      topContributingSignals: [
        { signalName: 'duplicate_utr_detected', signalValue: 'TRUE', importanceWeight: 0.75, interpretation: 'Multiple bank debits mapped to single order.' }
      ],
      classProbabilities: {
        DUPLICATE_PAYMENT: 0.9940,
        NORMAL: 0.0060
      }
    }
  }
];

export const api = {
  // --- INCIDENTS ---
  async getIncidents(params = {}) {
    const query = new URLSearchParams();
    if (params.status) query.append('status', params.status);
    if (params.severity) query.append('severity', params.severity);
    if (params.type) query.append('type', params.type);
    if (params.search) query.append('search', params.search);

    const queryString = query.toString() ? `?${query.toString()}` : '';
    const res = await request(`/incidents${queryString}`);
    
    if (res && res.content && Array.isArray(res.content)) {
      return res.content.map(mapIncidentToUi);
    }
    if (Array.isArray(res)) {
      return res.map(mapIncidentToUi);
    }
    return [];
  },

  async getIncidentById(incidentId) {
    const res = await request(`/incidents/${encodeURIComponent(incidentId)}`);
    return mapIncidentToUi(res);
  },

  async investigateIncident(incidentId) {
    return await request(`/incidents/${encodeURIComponent(incidentId)}/investigate`, {
      method: 'POST'
    });
  },

  async getAiReport(incidentId) {
    return await request(`/incidents/${encodeURIComponent(incidentId)}/ai-report`);
  },

  async getIncidentEvidence(incidentId) {
    return await request(`/incidents/${encodeURIComponent(incidentId)}/evidence`);
  },

  async getIncidentResolution(incidentId) {
    return await request(`/incidents/${encodeURIComponent(incidentId)}/resolution`);
  },

  async resolveIncident(incidentId, payload) {
    return await request(`/incidents/${encodeURIComponent(incidentId)}/resolve`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },

  // --- PAYMENTS & TIMELINE ---
  async getPayments(params = {}) {
    return request('/payments');
  },

  async getPaymentTimeline(paymentId) {
    return await request(`/payments/${encodeURIComponent(paymentId)}/timeline`);
  },

  async getDashboardSummary() {
    return await request('/dashboard/summary');
  },

  async getAuditEvents(entityId = null) {
    const endpoint = entityId ? `/audit/entity/${encodeURIComponent(entityId)}` : '/audit';
    return await request(endpoint);
  },

  async verifyAuditChain() {
    return await request('/audit/verify');
  },

  async getSystemHealth() {
    try {
      const res = await request('/health', { timeout: 3000 });
      return res;
    } catch (e) {
      return {
        status: 'OFFLINE',
        timestamp: new Date().toISOString(),
        services: {
          backend: { name: 'JAVA BACKEND', status: 'OFFLINE', label: 'Unavailable' },
          database: { name: 'MYSQL DATABASE', status: 'OFFLINE', label: 'Unavailable' },
          mlService: { name: 'ML SERVICE', status: 'OFFLINE', label: 'Unavailable' }
        },
        error: e.message
      };
    }
  }
};

function mapIncidentToUi(inc) {
  if (!inc) return null;
  return {
    id: inc.incidentId || inc.id,
    incidentId: inc.incidentId || inc.id,
    paymentId: inc.paymentId,
    orderId: inc.orderId || (inc.payment ? inc.payment.orderId : 'ORD-9901'),
    merchantId: inc.merchantId || (inc.payment ? inc.payment.merchantId : 'merch_default'),
    merchantName: inc.merchantName || (inc.merchantId ? inc.merchantId.replace('merch_', '').toUpperCase() : 'Merchant'),
    amount: inc.amount || (inc.payment ? inc.payment.amount : 8500.00),
    currency: inc.currency || (inc.payment ? inc.payment.currency : 'INR'),
    paymentMethod: inc.paymentMethod || (inc.payment ? inc.payment.paymentMethod : 'UPI'),
    customerName: inc.customerName || (inc.payment ? inc.payment.customerId : 'Customer'),
    customerEmail: inc.customerEmail || 'customer@example.com',
    severity: inc.severity || 'CRITICAL',
    caseStatus: inc.caseStatus || inc.status || 'OPEN',
    incidentType: inc.incidentType || inc.type || 'BANK_DEBIT_GATEWAY_FAILURE',
    title: inc.title || 'Payment Inconsistency Incident',
    description: inc.description || 'Discrepancy detected across banking and merchant states.',
    openedAt: inc.openedAt || new Date().toISOString(),
    moneyAtRisk: inc.moneyAtRisk || inc.amount || 0,
    isRetryProhibited: inc.isRetryProhibited !== undefined ? inc.isRetryProhibited : true,
    retryReason: inc.retryReason || 'Strict safety invariant active.',
    bank: inc.bank || { bankName: 'Core Bank Switch', status: 'SUCCESS', utr: '414960264709', latencyMs: 420 },
    gateway: inc.gateway || { gatewayName: 'Gateway PSP', status: 'PENDING', captureStatus: 'PENDING', latencyMs: 65000 },
    merchant: inc.merchant || { orderId: 'ORD-2026-00024', status: 'CANCELLED', fulfillmentStatus: 'CANCELLED' },
    webhook: inc.webhook || { deliveryStatus: 'DROPPED', httpStatusCode: 504, attemptCount: 3 },
    settlement: inc.settlement || { settlementStatus: 'ON_HOLD' },
    refund: inc.refund || { refundStatus: 'NOT_INITIATED' },
    aiReport: inc.aiReport || {
      whatHappened: "Your customer was charged, but the merchant did not receive confirmation before cancelling the order.",
      whyWeThinkThis: "The bank reports SUCCESS while the gateway remained PENDING and the merchant order was CANCELLED.",
      whatIsUncertain: "Unable to confirm whether the merchant inventory can be restored.",
      recommendedAction: "AUTO_REFUND_CUSTOMER",
      moneyAtRisk: inc.amount || 8500.00,
      confidence: 0.9924,
      isRetryProhibited: true,
      retryProhibitionReason: "Customer account debited. Blind retry is prohibited.",
      decisionFactors: ["Bank: SUCCESS", "Gateway: PENDING (Timeout)", "Merchant OMS: CANCELLED"]
    }
  };
}

export { FALLBACK_INCIDENTS };
export default api;
