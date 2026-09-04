import React from 'react';
import {
  Building2,
  Server,
  Store,
  Send,
  Scale,
  RotateCcw,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Clock,
  ShieldAlert
} from 'lucide-react';

export default function ConnectedEvidenceGraph({
  incident,
  selectedSource,
  onSelectSource
}) {
  if (!incident) return null;

  const isGhostDebit = incident.incidentType === 'BANK_DEBIT_GATEWAY_FAILURE' || incident.predictedRootCause === 'BANK_DEBIT_GATEWAY_FAILURE';
  const amountFormatted = Number(incident.amount || 4500).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
  const bankUtr = incident.bank?.utr || 'UTR984102947101';
  const orderId = incident.orderId || 'ORD_9841_PAY';

  const sources = [
    {
      id: 'BANK',
      name: '1. Core Bank Switch',
      icon: Building2,
      status: incident.bank?.status || 'SUCCESS',
      badge: 'FUNDS DEBITED',
      amountTag: `₹${amountFormatted}`,
      subStatus: `UTR: ${bankUtr}`,
      latency: `${incident.bank?.latencyMs || 420}ms (ISO-8583 200 OK)`,
      isClash: false,
      color: '#10b981',
      hasContradiction: true,
      contradictionText: 'Debited at Bank switch, but Gateway reported Timeout.'
    },
    {
      id: 'GATEWAY',
      name: '2. Gateway Aggregator',
      icon: Server,
      status: incident.gateway?.status || 'FAILED',
      badge: 'NOT CAPTURED',
      amountTag: '₹0.00 Settled',
      subStatus: `Auth: ${incident.gateway?.authStatus || 'FAILED'} • Capture: ${incident.gateway?.captureStatus || 'NOT_REQUESTED'}`,
      latency: `${(incident.gateway?.latencyMs || 65000) / 1000}s Gateway Timeout`,
      isClash: true,
      color: '#ef4444',
      hasContradiction: true,
      contradictionText: '65s Timeout: Failed to record authorization.'
    },
    {
      id: 'MERCHANT',
      name: '3. Merchant OMS',
      icon: Store,
      status: incident.merchant?.status || 'CANCELLED',
      badge: 'UNFULFILLED',
      amountTag: 'Order Void',
      subStatus: `Order: ${orderId} (${incident.merchant?.fulfillmentStatus || 'UNFULFILLED'})`,
      latency: 'Checkout Session Expired',
      isClash: true,
      color: '#ef4444',
      hasContradiction: true,
      contradictionText: 'Cart released without food delivery.'
    },
    {
      id: 'WEBHOOK',
      name: '4. Webhook Pipeline',
      icon: Send,
      status: incident.webhook?.deliveryStatus || 'DROPPED',
      badge: 'HTTP 504',
      amountTag: '3 Attempts',
      subStatus: `HTTP ${incident.webhook?.httpStatusCode || 504} (${incident.webhook?.attemptCount || 3} delivery attempts)`,
      latency: 'Upstream Gateway Timeout',
      isClash: true,
      color: '#f59e0b',
      hasContradiction: false
    },
    {
      id: 'SETTLEMENT',
      name: '5. Settlement Ledger',
      icon: Scale,
      status: incident.settlement?.settlementStatus || 'NOT_FOUND',
      badge: 'ON HOLD',
      amountTag: 'Escrow Trapped',
      subStatus: incident.settlement?.settlementStatus === 'NOT_FOUND' ? 'No Settlement Batch Entry' : 'Batch Held in Escrow',
      latency: 'T+1 Nodal Settlement Paused',
      isClash: false,
      color: '#06b6d4',
      hasContradiction: false
    },
    {
      id: 'REFUND',
      name: '6. Refund Engine',
      icon: RotateCcw,
      status: incident.refund?.refundStatus || 'NOT_INITIATED',
      badge: 'REVERSAL READY',
      amountTag: `₹${amountFormatted}`,
      subStatus: 'Awaiting Operator Instant Reversal',
      latency: 'Instant UPI ARN Engine',
      isClash: false,
      color: '#8b5cf6',
      hasContradiction: false
    }
  ];

  return (
    <div style={{
      background: '#09090b',
      border: '1px solid var(--border-subtle)',
      borderRadius: '14px',
      padding: '20px',
      display: 'flex',
      flexDirection: 'column',
      gap: '14px'
    }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <h3 className="font-display" style={{ fontSize: '0.98rem', fontWeight: 800, color: '#ffffff' }}>
              6 Connected Authoritative Evidence Sources
            </h3>
            <span className="badge badge-outline font-mono" style={{ fontSize: '0.62rem' }}>
              CLICK NODE TO VIEW RAW PAYLOAD & HASH
            </span>
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
            Real-time cross-system verification matrix across banking switches, PSP gateway, and merchant OMS
          </div>
        </div>
      </div>

      {/* Critical Contradictions Callout Bar */}
      {isGhostDebit && (
        <div style={{
          background: 'rgba(239, 68, 68, 0.06)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          borderRadius: '8px',
          padding: '10px 14px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          flexWrap: 'wrap'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#ef4444', fontWeight: 700, fontSize: '0.78rem' }}>
            <AlertTriangle size={15} />
            CRITICAL CONTRADICTIONS DETECTED:
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', fontSize: '0.74rem' }}>
            <span className="badge" style={{ background: '#18181b', color: '#ffffff', border: '1px solid var(--border-subtle)' }}>
              1. Bank SUCCESS (₹{amountFormatted} Debited) vs Gateway FAILED (65s Timeout)
            </span>
            <span className="badge" style={{ background: '#18181b', color: '#ffffff', border: '1px solid var(--border-subtle)' }}>
              2. Bank Debited vs Merchant OMS CANCELLED (Unfulfilled)
            </span>
          </div>
        </div>
      )}

      {/* Clean 6-Node Grid: 3 columns x 2 rows */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 1fr)',
        gap: '12px'
      }}>
        {sources.map(src => {
          const Icon = src.icon;
          const isSelected = selectedSource === src.id;

          return (
            <div
              key={src.id}
              onClick={() => onSelectSource && onSelectSource(src.id)}
              style={{
                background: isSelected ? 'rgba(255, 255, 255, 0.08)' : '#040404',
                border: `1px solid ${isSelected ? '#ffffff' : src.isClash ? 'rgba(239, 68, 68, 0.4)' : 'var(--border-subtle)'}`,
                borderRadius: '10px',
                padding: '14px',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
                position: 'relative',
                overflow: 'hidden'
              }}
              className="surface-interactive"
            >
              {/* Highlight bar for clashing states */}
              {src.isClash && (
                <div style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  right: 0,
                  height: '2px',
                  background: src.color
                }} />
              )}

              {/* Node Header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div style={{
                    width: '26px',
                    height: '26px',
                    borderRadius: '6px',
                    background: 'rgba(255, 255, 255, 0.06)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: src.color
                  }}>
                    <Icon size={14} />
                  </div>
                  <div className="font-display" style={{ fontSize: '0.84rem', fontWeight: 700, color: '#ffffff' }}>
                    {src.name}
                  </div>
                </div>

                <span style={{
                  fontSize: '0.68rem',
                  fontWeight: 800,
                  color: src.color,
                  textTransform: 'uppercase',
                  background: 'rgba(255, 255, 255, 0.05)',
                  padding: '2px 6px',
                  borderRadius: '4px'
                }}>
                  {src.status}
                </span>
              </div>

              {/* Node Details */}
              <div className="font-mono" style={{ fontSize: '0.74rem', color: '#ffffff', fontWeight: 600, marginBottom: '2px' }}>
                {src.subStatus}
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.68rem', color: 'var(--text-dim)', marginTop: '6px' }}>
                <span>Latency: <strong style={{ color: '#d4d4d8' }}>{src.latency}</strong></span>
                <span style={{ color: isSelected ? '#ffffff' : 'var(--text-muted)', fontWeight: 600 }}>view proof →</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
