import React from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  Lock,
  ArrowRight,
  Sparkles,
  TrendingUp,
  Activity,
  Layers,
  DollarSign,
  Info
} from 'lucide-react';

export default function PaymentTruthSection({
  incident,
  onOpenResolution,
  onTriggerReanalysis,
  onSelectEvidenceSource,
  selectedQuestion,
  onSelectQuestion
}) {
  if (!incident) return null;

  const ai = incident.aiReport || {};
  const isBankDebited = incident.bank?.status === 'SUCCESS' || incident.bank?.status === 'DEBITED';
  const isMerchantCancelled = incident.merchant?.status === 'CANCELLED';
  const isGhostDebit = incident.incidentType === 'BANK_DEBIT_GATEWAY_FAILURE';
  const isMissingWebhook = incident.incidentType === 'MISSING_WEBHOOK';

  const confidenceScore = ai.confidence ? (ai.confidence * 100).toFixed(1) : '99.2';

  return (
    <div style={{
      background: '#09090b',
      border: '1px solid rgba(255, 255, 255, 0.12)',
      borderRadius: '14px',
      padding: '20px 24px',
      display: 'flex',
      flexDirection: 'column',
      gap: '18px',
      position: 'relative'
    }}>
      {/* Top Bar: Title & Action Buttons (Aligned, Never Cut Off) */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '14px',
        borderBottom: '1px solid var(--border-subtle)',
        paddingBottom: '16px'
      }}>
        {/* Left Title & Status */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <span className="badge badge-white font-mono" style={{ fontSize: '0.72rem', fontWeight: 800 }}>
              {incident.paymentId}
            </span>
            <span className="badge badge-outline" style={{ fontSize: '0.68rem' }}>
              {incident.paymentMethod || 'UPI'}
            </span>
            <span className={`badge ${
              incident.severity === 'CRITICAL' ? 'badge-critical' : 'badge-high'
            }`} style={{ fontSize: '0.68rem' }}>
              {incident.incidentType?.replace(/_/g, ' ')}
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'baseline', gap: '12px' }}>
            <div className="font-display" style={{ fontSize: '2.2rem', fontWeight: 800, color: '#ffffff', letterSpacing: '-0.03em' }}>
              ₹{Number(incident.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
            </div>
            <span style={{ fontSize: '0.85rem', fontWeight: 700, color: isGhostDebit ? '#ef4444' : '#10b981' }}>
              {isGhostDebit ? 'STATUS: UNCLEAR (GHOST DEBIT)' : isMissingWebhook ? 'STATUS: PENDING MERCHANT ACK' : 'STATUS: OPEN INVESTIGATION'}
            </span>
          </div>
        </div>

        {/* Right Action Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            onClick={onTriggerReanalysis}
            className="btn btn-outline-white btn-sm"
            style={{ padding: '8px 16px', fontSize: '0.82rem', borderRadius: '8px' }}
          >
            <Sparkles size={14} style={{ color: '#06b6d4' }} />
            AI Re-Assess
          </button>
          <button
            onClick={onOpenResolution}
            className="btn btn-white btn-sm"
            style={{ padding: '8px 18px', fontSize: '0.82rem', fontWeight: 700, borderRadius: '8px' }}
          >
            <ShieldCheck size={15} />
            Resolve Incident
          </button>
        </div>
      </div>

      {/* Forensic Verdict Banner */}
      <div style={{
        background: 'rgba(255, 255, 255, 0.03)',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        borderRadius: '10px',
        padding: '14px 16px',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '12px'
      }}>
        <div style={{
          width: '26px',
          height: '26px',
          borderRadius: '6px',
          background: isGhostDebit ? 'rgba(239, 68, 68, 0.15)' : 'rgba(6, 182, 212, 0.15)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: isGhostDebit ? '#ef4444' : '#06b6d4',
          flexShrink: 0,
          marginTop: '2px'
        }}>
          <Info size={15} />
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2px' }}>
            <span style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase' }}>
              What Happened To The Money?
            </span>
            {(ai.gemini_explanation || ai.geminiExplanation) && (
              <span className="badge" style={{ fontSize: '0.62rem', background: 'rgba(6, 182, 212, 0.12)', color: '#22d3ee', border: '1px solid rgba(6, 182, 212, 0.35)', display: 'inline-flex', alignItems: 'center', gap: '4px', padding: '1px 6px' }}>
                <Sparkles size={10} /> Gemini Synthesized
              </span>
            )}
          </div>
          <div style={{ fontSize: '0.92rem', color: '#ffffff', lineHeight: 1.5, fontWeight: 500 }}>
            {ai.whatHappened || 'Your customer was debited ₹8,500 via UPI from their bank account, but an upstream network timeout prevented the merchant from confirming the payment before the checkout session expired.'}
          </div>
        </div>
      </div>

      {/* 5 Core Pillars in a Clean Balanced Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(5, 1fr)',
        gap: '10px'
      }}>
        {/* 1. Money Movement */}
        <div
          onClick={() => onSelectEvidenceSource && onSelectEvidenceSource('BANK')}
          style={{ background: '#040404', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 12px', cursor: 'pointer' }}
          className="surface-interactive"
        >
          <div style={{ fontSize: '0.66rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
            1. Money Movement
          </div>
          <div className="font-display" style={{ fontSize: '0.92rem', fontWeight: 800, color: '#10b981', marginBottom: '2px' }}>
            {isBankDebited ? `Debited ₹${Number(incident.amount || 0).toLocaleString('en-IN')}` : 'Not Debited'}
          </div>
          <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
            Held at Nodal Switch
          </div>
        </div>

        {/* 2. Order State */}
        <div
          onClick={() => onSelectEvidenceSource && onSelectEvidenceSource('MERCHANT')}
          style={{ background: '#040404', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 12px', cursor: 'pointer' }}
          className="surface-interactive"
        >
          <div style={{ fontSize: '0.66rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
            2. Order State
          </div>
          <div className="font-display" style={{ fontSize: '0.92rem', fontWeight: 800, color: isMerchantCancelled ? '#ef4444' : '#ffffff', marginBottom: '2px' }}>
            {incident.merchant?.status || 'CANCELLED'}
          </div>
          <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
            Cart Released
          </div>
        </div>

        {/* 3. Payment State */}
        <div
          onClick={() => onSelectEvidenceSource && onSelectEvidenceSource('GATEWAY')}
          style={{ background: '#040404', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 12px', cursor: 'pointer' }}
          className="surface-interactive"
        >
          <div style={{ fontSize: '0.66rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
            3. Payment State
          </div>
          <div className="font-display" style={{ fontSize: '0.92rem', fontWeight: 800, color: isGhostDebit ? '#ef4444' : '#f59e0b', marginBottom: '2px' }}>
            {isGhostDebit ? 'GHOST DEBIT' : 'DIVERGED'}
          </div>
          <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
            State Clash Detected
          </div>
        </div>

        {/* 4. ML Confidence */}
        <div
          style={{ background: '#040404', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 12px' }}
        >
          <div style={{ fontSize: '0.66rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
            4. ML Confidence
          </div>
          <div className="font-display" style={{ fontSize: '0.92rem', fontWeight: 800, color: '#10b981', marginBottom: '2px' }}>
            {confidenceScore}%
          </div>
          <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
            Random Forest
          </div>
        </div>

        {/* 5. Recommended Action */}
        <div
          onClick={onOpenResolution}
          style={{ background: '#040404', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: '8px', padding: '10px 12px', cursor: 'pointer' }}
          className="surface-interactive"
        >
          <div style={{ fontSize: '0.66rem', fontWeight: 700, color: incident.isRetryProhibited ? '#ef4444' : '#10b981', textTransform: 'uppercase', marginBottom: '4px' }}>
            5. Recommended Action
          </div>
          <div className="font-display" style={{ fontSize: '0.88rem', fontWeight: 800, color: '#ffffff', marginBottom: '2px', textTransform: 'uppercase' }}>
            {(ai.recommendedAction || 'AUTO_REFUND_CUSTOMER').replace(/_/g, ' ')}
          </div>
          <div style={{ fontSize: '0.68rem', color: incident.isRetryProhibited ? '#fca5a5' : '#86efac', fontWeight: 600 }}>
            {incident.isRetryProhibited ? 'Retry Prohibited' : 'Retry Permitted'}
          </div>
        </div>
      </div>
    </div>
  );
}
