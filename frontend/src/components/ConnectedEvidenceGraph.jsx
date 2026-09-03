import React from 'react';
import {
  Building2,
  Server,
  Store,
  Send,
  Scale,
  RotateCcw
} from 'lucide-react';

export default function ConnectedEvidenceGraph({
  incident,
  selectedSource,
  onSelectSource
}) {
  if (!incident) return null;

  const isGhostDebit = incident.incidentType === 'BANK_DEBIT_GATEWAY_FAILURE';

  const sources = [
    {
      id: 'BANK',
      name: '1. Core Bank Switch',
      icon: Building2,
      status: incident.bank?.status || 'SUCCESS',
      subStatus: `UTR: ${incident.bank?.utr || '414960264709'}`,
      latency: `${incident.bank?.latencyMs || 420}ms`,
      isClash: false,
      color: '#10b981'
    },
    {
      id: 'GATEWAY',
      name: '2. Gateway Aggregator',
      icon: Server,
      status: incident.gateway?.status || 'PENDING',
      subStatus: `Auth: ${incident.gateway?.authStatus || 'TIMEOUT'}`,
      latency: `${(incident.gateway?.latencyMs || 65000) / 1000}s Timeout`,
      isClash: isGhostDebit,
      color: isGhostDebit ? '#ef4444' : '#10b981'
    },
    {
      id: 'MERCHANT',
      name: '3. Merchant OMS',
      icon: Store,
      status: incident.merchant?.status || 'CANCELLED',
      subStatus: `Order: ${incident.orderId || 'ORD-2026-00024'}`,
      latency: 'Session Expired',
      isClash: incident.merchant?.status === 'CANCELLED',
      color: incident.merchant?.status === 'CANCELLED' ? '#ef4444' : '#10b981'
    },
    {
      id: 'WEBHOOK',
      name: '4. Webhook Pipeline',
      icon: Send,
      status: incident.webhook?.deliveryStatus || 'DROPPED',
      subStatus: `HTTP ${incident.webhook?.httpStatusCode || 504} (3 attempts)`,
      latency: '504 Timeout',
      isClash: incident.webhook?.deliveryStatus === 'DROPPED',
      color: incident.webhook?.deliveryStatus === 'DROPPED' ? '#f59e0b' : '#10b981'
    },
    {
      id: 'SETTLEMENT',
      name: '5. Settlement Ledger',
      icon: Scale,
      status: incident.settlement?.settlementStatus || 'ON_HOLD',
      subStatus: 'Batch Held in Escrow',
      latency: 'T+1 Nodal',
      isClash: false,
      color: '#06b6d4'
    },
    {
      id: 'REFUND',
      name: '6. Refund Engine',
      icon: RotateCcw,
      status: incident.refund?.refundStatus || 'NOT_INITIATED',
      subStatus: 'Ready for Reversal',
      latency: 'Instant ARN',
      isClash: false,
      color: '#8b5cf6'
    }
  ];

  return (
    <div style={{
      background: '#09090b',
      border: '1px solid var(--border-subtle)',
      borderRadius: '14px',
      padding: '20px'
    }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <h3 className="font-display" style={{ fontSize: '0.95rem', fontWeight: 800, color: '#ffffff' }}>
              6 Connected Authoritative Evidence Sources
            </h3>
            <span className="badge badge-outline" style={{ fontSize: '0.62rem' }}>
              Click Node To Inspect Raw Log
            </span>
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
            Real-time cross-system verification matrix across banking, gateway, and merchant nodes
          </div>
        </div>
      </div>

      {/* Clean 6-Node Grid: 3 columns x 2 rows (Balanced on all screens) */}
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
                border: `1px solid ${isSelected ? '#ffffff' : src.isClash ? 'rgba(239, 68, 68, 0.35)' : 'var(--border-subtle)'}`,
                borderRadius: '10px',
                padding: '12px 14px',
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
                  background: '#ef4444'
                }} />
              )}

              {/* Node Header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <div style={{
                    width: '24px',
                    height: '24px',
                    borderRadius: '6px',
                    background: 'rgba(255, 255, 255, 0.06)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: src.color
                  }}>
                    <Icon size={13} />
                  </div>
                  <div className="font-display" style={{ fontSize: '0.82rem', fontWeight: 700, color: '#ffffff' }}>
                    {src.name}
                  </div>
                </div>

                <span style={{
                  fontSize: '0.68rem',
                  fontWeight: 700,
                  color: src.color,
                  textTransform: 'uppercase'
                }}>
                  {src.status}
                </span>
              </div>

              {/* Node Details */}
              <div className="font-mono" style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginBottom: '4px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {src.subStatus}
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.68rem', color: 'var(--text-dim)' }}>
                <span>Latency: {src.latency}</span>
                <span style={{ color: isSelected ? '#ffffff' : 'var(--text-dim)', fontWeight: 600 }}>inspect proof →</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
