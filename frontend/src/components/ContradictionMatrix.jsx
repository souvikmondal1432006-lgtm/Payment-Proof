import React from 'react';
import {
  AlertTriangle,
  XCircle,
  CheckCircle2,
  ShieldAlert,
  ArrowRight,
  Split,
  Layers,
  HelpCircle
} from 'lucide-react';

export default function ContradictionMatrix({ incident, onSelectContradiction }) {
  if (!incident) return null;

  const isGhostDebit = incident.incidentType === 'BANK_DEBIT_GATEWAY_FAILURE';
  const isMissingWebhook = incident.incidentType === 'MISSING_WEBHOOK';

  const contradictions = [
    {
      id: 'contra_01',
      type: 'BANK_VS_MERCHANT_CLASH',
      title: 'State Clash: Bank SUCCESS vs Merchant CANCELLED',
      severity: 'CRITICAL',
      sourceA: { name: 'Core Bank Switch', state: 'SUCCESS (₹8,500 DEBITED)', utr: incident.bank?.utr || '414960264709' },
      sourceB: { name: 'Merchant OMS', state: 'CANCELLED (CART RELEASED)', orderId: incident.orderId || 'ORD-2026-00024' },
      divergenceSummary: 'Customer account was debited, but the merchant never received confirmation before session expiry.',
      remedy: 'Customer holds valid bank debit proof. Automated instant refund required to prevent financial loss.'
    },
    {
      id: 'contra_02',
      type: 'GATEWAY_TIMEOUT_CLASH',
      title: 'Latency Clash: Gateway 65s Timeout vs Bank 420ms Clearance',
      severity: 'HIGH',
      sourceA: { name: 'Bank Switch', state: '420ms Latency (Approved)', utr: incident.bank?.utr },
      sourceB: { name: 'Gateway Aggregator', state: '65,000ms Latency (Timed Out)', orderId: 'PSP Timeout' },
      divergenceSummary: 'Core banking cleared the debit in 420ms, but asynchronous network socket between PSP and Gateway timed out after 65 seconds.',
      remedy: 'Nodal switch holds the captured funds in transit escrow.'
    }
  ];

  return (
    <div style={{
      background: '#09090b',
      border: '1px solid var(--border-subtle)',
      borderRadius: '14px',
      padding: '20px'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <h3 className="font-display" style={{ fontSize: '1rem', fontWeight: 800, color: '#ffffff' }}>
              Detected Subsystem Contradictions & Clashes
            </h3>
            <span className="badge badge-critical" style={{ fontSize: '0.65rem' }}>
              {contradictions.length} Active Clashes
            </span>
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
            Authoritative contradiction analysis identifying multi-party state disagreements
          </div>
        </div>
      </div>

      {/* Contradiction Cards */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {contradictions.map(contra => (
          <div
            key={contra.id}
            onClick={() => onSelectContradiction && onSelectContradiction(contra)}
            style={{
              background: '#040404',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              borderRadius: '10px',
              padding: '14px 16px',
              cursor: 'pointer',
              transition: 'all 0.15s',
              position: 'relative',
              overflow: 'hidden'
            }}
            className="surface-interactive"
          >
            {/* Red Accent Marker */}
            <div style={{
              position: 'absolute',
              left: 0,
              top: 0,
              bottom: 0,
              width: '3px',
              background: '#ef4444'
            }} />

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <AlertTriangle size={15} style={{ color: '#ef4444' }} />
                <span className="font-display" style={{ fontSize: '0.88rem', fontWeight: 700, color: '#ffffff' }}>
                  {contra.title}
                </span>
              </div>
              <span className="badge badge-critical" style={{ fontSize: '0.65rem' }}>
                {contra.severity}
              </span>
            </div>

            {/* Split Comparison Box */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: '1fr auto 1fr',
              alignItems: 'center',
              gap: '10px',
              background: 'rgba(255, 255, 255, 0.02)',
              border: '1px solid var(--border-subtle)',
              borderRadius: '8px',
              padding: '10px 12px',
              marginBottom: '10px'
            }}>
              {/* Left Side (Source A) */}
              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>
                  {contra.sourceA.name}
                </div>
                <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#10b981' }}>
                  {contra.sourceA.state}
                </div>
              </div>

              {/* Conflict Divider */}
              <div style={{
                width: '24px',
                height: '24px',
                borderRadius: '50%',
                background: 'rgba(239, 68, 68, 0.15)',
                color: '#ef4444',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '0.75rem',
                fontWeight: 900
              }}>
                ≠
              </div>

              {/* Right Side (Source B) */}
              <div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>
                  {contra.sourceB.name}
                </div>
                <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#ef4444' }}>
                  {contra.sourceB.state}
                </div>
              </div>
            </div>

            <div style={{ fontSize: '0.78rem', color: '#d4d4d8', lineHeight: 1.4, marginBottom: '6px' }}>
              <strong style={{ color: '#ffffff' }}>Divergence: </strong>
              {contra.divergenceSummary}
            </div>

            <div style={{ fontSize: '0.74rem', color: '#fca5a5', lineHeight: 1.35 }}>
              <strong style={{ color: '#ffffff' }}>Remedy: </strong>
              {contra.remedy}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
