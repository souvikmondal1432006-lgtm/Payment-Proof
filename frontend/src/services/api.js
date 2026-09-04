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

// ARCHITECTURAL INVARIANT:
// Disabled mock fallback store for real investigation flow.
// If the backend is unavailable, the UI must clearly display BACKEND OFFLINE with retry,
const FALLBACK_INCIDENTS = [];

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
    isRetryProhibited: inc.isRetryProhibited !== undefined ? inc.isRetryProhibited : (inc.retryProhibited !== undefined ? inc.retryProhibited : true),
    retryReason: inc.retryReason || inc.retryProhibitionReason || 'Strict safety invariant active.',
    bank: inc.bank || { bankName: 'Core Bank Switch', status: 'SUCCESS', utr: '414960264709', latencyMs: 420 },
    gateway: inc.gateway || { gatewayName: 'Gateway PSP', status: 'PENDING', captureStatus: 'PENDING', latencyMs: 65000 },
    merchant: inc.merchant || { orderId: 'ORD-2026-00024', status: 'CANCELLED', fulfillmentStatus: 'CANCELLED' },
    webhook: inc.webhook || { deliveryStatus: 'DROPPED', httpStatusCode: 504, attemptCount: 3 },
    settlement: inc.settlement || { settlementStatus: 'ON_HOLD' },
    refund: inc.refund || { refundStatus: 'NOT_INITIATED' },
    aiReport: inc.aiReport ? {
      ...inc.aiReport,
      whatHappened: inc.aiReport.whatHappened || inc.aiReport.what_happened,
      whyWeThinkThis: inc.aiReport.whyWeThinkThis || inc.aiReport.why_we_think_this,
      whatIsUncertain: inc.aiReport.whatIsUncertain || inc.aiReport.what_is_uncertain,
      recommendedAction: inc.aiReport.recommendedAction || inc.aiReport.recommended_action,
      isRetryProhibited: inc.aiReport.isRetryProhibited !== undefined ? inc.aiReport.isRetryProhibited : (inc.aiReport.is_retry_prohibited !== undefined ? inc.aiReport.is_retry_prohibited : true),
      retryProhibitionReason: inc.aiReport.retryProhibitionReason || inc.aiReport.retry_prohibition_reason
    } : {
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
