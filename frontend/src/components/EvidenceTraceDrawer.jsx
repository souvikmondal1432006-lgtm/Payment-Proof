import React from 'react';
import {
  X,
  ShieldCheck,
  FileCode,
  CheckCircle2,
  Copy,
  ExternalLink,
  Lock,
  Building2,
  Server,
  Store,
  Send,
  Scale,
  RotateCcw
} from 'lucide-react';

export default function EvidenceTraceDrawer({
  isOpen,
  onClose,
  selectedSource,
  selectedEvent,
  incident
}) {
  if (!isOpen || (!selectedSource && !selectedEvent)) return null;

  const [copiedHash, setCopiedHash] = React.useState(false);

  const getSourceData = () => {
    const src = selectedSource || (selectedEvent ? selectedEvent.sourceId : 'BANK');

    const amountVal = Number(incident?.amount || 4500.00);
    const amountFormatted = amountVal.toLocaleString('en-IN', { minimumFractionDigits: 2 });
    const amountIso = String(Math.round(amountVal * 100)).padStart(12, '0');
    const bankUtr = incident?.bank?.utr || 'UTR984102947101';
    const paymentId = incident?.paymentId || 'pay_test_001';
    const orderId = incident?.orderId || 'ORD_9841_PAY';
    const merchantName = incident?.merchantName || 'Swiggy India OMS';

    switch (src) {
      case 'BANK':
        return {
          title: 'Core Banking Switch Proof (ISO-8583 Audit Log)',
          sourceName: 'Core Banking Network (HDFC / NPCI Switch)',
          status: 'SUCCESS (200 OK)',
          color: '#10b981',
          utr: bankUtr,
          timestamp: '2026-08-30T10:43:20.420Z',
          checksum: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
          payload: `MTI: 0200 (Financial Transaction Request / Completion)
ProcessingCode: 000000 (Goods & Services Debit)
Amount: ${amountIso} (INR ${amountFormatted})
TransmissionDateTime: 20260830104320
SystemTraceAuditNumber (STAN): 414960
RetrievalReferenceNumber (RRN/UTR): ${bankUtr}
CardAcceptorId: ${merchantName.toUpperCase().replace(/\s+/g, '_')}
ResponseCode: 00 (APPROVED / COMPLETED)
NetworkLatency: 420ms`
        };

      case 'GATEWAY':
        return {
          title: 'Gateway Aggregator Telemetry Trace',
          sourceName: 'Razorpay PSP Pipeline',
          status: 'FAILED / TIMED OUT',
          color: '#ef4444',
          utr: paymentId,
          timestamp: '2026-08-30T10:44:25.000Z',
          checksum: '8f4811e03a985f524c568c8194432168926941584742a7ec72922756a12b4899',
          payload: `{
  "payment_id": "${paymentId}",
  "order_id": "${orderId}",
  "status": "failed",
  "auth_status": "FAILED",
  "capture_status": "NOT_REQUESTED",
  "latency_ms": 65000,
  "error_code": "GATEWAY_TIMEOUT_AFTER_65S",
  "client_ip": "103.101.148.76"
}`
        };

      case 'MERCHANT':
        return {
          title: 'Merchant Order Management (OMS) Record',
          sourceName: merchantName,
          status: 'CANCELLED',
          color: '#ef4444',
          utr: orderId,
          timestamp: '2026-08-30T10:44:30.000Z',
          checksum: '6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b',
          payload: `{
  "order_id": "${orderId}",
  "merchant_id": "${incident?.merchantId || 'merch_swiggy_ind'}",
  "order_status": "CANCELLED",
  "fulfillment_status": "CANCELLED",
  "cancellation_reason": "SESSION_TIMEOUT_NO_PROOF",
  "items": [{"item": "Food & Beverage Order", "amount": ${amountVal.toFixed(2)}}]
}`
        };

      case 'WEBHOOK':
        return {
          title: 'Webhook Event Dispatcher Delivery Trail',
          sourceName: 'HMAC Webhook Engine',
          status: 'DROPPED (HTTP 504)',
          color: '#f59e0b',
          utr: 'whk_delivery_9901',
          timestamp: '2026-08-30T10:45:00.000Z',
          checksum: 'd4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35',
          payload: `TargetURL: https://api.swiggy.com/payments/webhook
Attempts: 3 of 3
HTTPStatusCode: 504 Gateway Timeout
DeliveryStatus: DROPPED
Signature: sha256=a1b2c3d4e5f6...`
        };

      case 'SETTLEMENT':
        return {
          title: 'Merchant Escrow & Settlement Ledger',
          sourceName: 'Nodal Clearing Engine',
          status: 'ON_HOLD',
          color: '#06b6d4',
          utr: 'stl_batch_20260830',
          timestamp: '2026-08-30T10:45:10.000Z',
          checksum: '4e07408562bedb8b60ce05c1decfe3ad16b72230967de01f640b7e4729b49fce',
          payload: `GrossAmount: INR ${amountFormatted}
MDR_Fee: INR ${(amountVal * 0.02).toFixed(2)} (2.0%)
GST: INR ${(amountVal * 0.02 * 0.18).toFixed(2)} (18% on MDR)
NetSettled: INR 0.00 (Payout Batch ON_HOLD due to active incident case)`
        };

      default:
        return {
          title: 'Refund & Reversal Engine Trace',
          sourceName: 'Instant Clearing Reversal',
          status: 'READY FOR DISPATCH',
          color: '#8b5cf6',
          utr: 'ref_init_001',
          timestamp: '2026-08-30T10:45:15.000Z',
          checksum: '4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a',
          payload: `Action: INITIATE_AUTO_REFUND_CUSTOMER
Destination: Original UPI VPA / Bank Account
Amount: INR ${amountFormatted}
Status: AWAITING_OPERATOR_SIGN_OFF`
        };
    }
  };

  const data = getSourceData();

  const handleCopyChecksum = () => {
    navigator.clipboard.writeText(data.checksum);
    setCopiedHash(true);
    setTimeout(() => setCopiedHash(false), 2000);
  };

  return (
    <div
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0, 0, 0, 0.85)',
        backdropFilter: 'blur(8px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        padding: '16px',
        overflowY: 'auto'
      }}
    >
      <div className="surface-card animate-fade-in" style={{
        width: '100%',
        maxWidth: '620px',
        maxHeight: 'calc(100vh - 32px)',
        display: 'flex',
        flexDirection: 'column',
        background: '#070707',
        border: '1px solid var(--border-strong)',
        boxShadow: '0 20px 60px rgba(0,0,0,0.95)',
        borderRadius: '14px',
        overflow: 'hidden'
      }}>
        {/* Header */}
        <div style={{
          padding: '14px 18px',
          borderBottom: '1px solid var(--border-subtle)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: '#040404',
          flexShrink: 0
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{
              width: '30px',
              height: '30px',
              borderRadius: '6px',
              background: '#ffffff',
              color: '#000000',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0
            }}>
              <ShieldCheck size={18} />
            </div>
            <div>
              <h2 className="font-display" style={{ fontSize: '1.05rem', fontWeight: 800, color: '#ffffff' }}>
                {data.title}
              </h2>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                {data.sourceName}
              </div>
            </div>
          </div>

          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              padding: '6px',
              borderRadius: '6px'
            }}
            title="Close"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div style={{ padding: '18px', overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '14px' }}>
          
          {/* Metadata Row */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '10px',
            background: '#0c0c0e',
            border: '1px solid var(--border-subtle)',
            borderRadius: '8px',
            padding: '12px'
          }}>
            <div>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Status</div>
              <div style={{ fontSize: '0.85rem', fontWeight: 700, color: data.color }}>{data.status}</div>
            </div>
            <div>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Reference / UTR</div>
              <div className="font-mono" style={{ fontSize: '0.82rem', fontWeight: 700, color: '#ffffff' }}>{data.utr}</div>
            </div>
          </div>

          {/* Raw Payload Log */}
          <div>
            <div style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '6px' }}>
              Raw Protocol Telemetry Payload:
            </div>
            <pre className="font-mono" style={{
              background: '#020202',
              border: '1px solid var(--border-subtle)',
              borderRadius: '8px',
              padding: '12px',
              fontSize: '0.75rem',
              color: '#d4d4d8',
              overflowX: 'auto',
              whiteSpace: 'pre-wrap',
              lineHeight: 1.45
            }}>
              {data.payload}
            </pre>
          </div>

          {/* SHA-256 Checksum */}
          <div style={{
            background: '#040404',
            border: '1px solid var(--border-subtle)',
            borderRadius: '8px',
            padding: '10px 12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '10px'
          }}>
            <div style={{ overflow: 'hidden' }}>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>
                Cryptographic SHA-256 Checksum
              </div>
              <div className="font-mono" style={{ fontSize: '0.72rem', color: '#ffffff', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {data.checksum}
              </div>
            </div>

            <button
              onClick={handleCopyChecksum}
              className="btn btn-outline-white btn-sm"
              style={{ fontSize: '0.7rem', padding: '4px 8px', flexShrink: 0 }}
            >
              <Copy size={12} />
              {copiedHash ? 'Copied' : 'Copy'}
            </button>
          </div>

        </div>

        {/* Footer */}
        <div style={{
          padding: '10px 18px',
          borderTop: '1px solid var(--border-subtle)',
          background: '#040404',
          display: 'flex',
          justifyContent: 'flex-end',
          flexShrink: 0
        }}>
          <button
            onClick={onClose}
            className="btn btn-white btn-sm"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
