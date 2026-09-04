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
  Info,
  Cpu,
  Brain,
  FileText,
  Clock,
  Zap,
  Check
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
  const isGhostDebit = incident.incidentType === 'BANK_DEBIT_GATEWAY_FAILURE' || incident.predictedRootCause === 'BANK_DEBIT_GATEWAY_FAILURE';
  const isOpen = incident.caseStatus === 'OPEN';
  const isResolved = incident.caseStatus === 'RESOLVED';

  const amountFormatted = Number(incident.amount || 4500).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });

  const confidenceScore = incident.confidence
    ? (incident.confidence > 1 ? incident.confidence : incident.confidence * 100).toFixed(1)
    : (ai.confidence ? (ai.confidence > 1 ? ai.confidence : ai.confidence * 100).toFixed(1) : '97.5');

  const anomalyScore = incident.anomalyScore !== undefined && incident.anomalyScore !== null
    ? Number(incident.anomalyScore).toFixed(4)
    : '0.9750';

  const bankUtr = incident.bank?.utr || 'UTR984102947101';
  const orderId = incident.orderId || 'ORD_9841_PAY';
  const merchantName = incident.merchantName || 'Swiggy';

  return (
    <div style={{
      background: '#09090b',
      border: isOpen ? '1px solid rgba(245, 158, 11, 0.4)' : '1px solid rgba(255, 255, 255, 0.12)',
      borderRadius: '14px',
      padding: '22px 26px',
      display: 'flex',
      flexDirection: 'column',
      gap: '20px',
      position: 'relative',
      boxShadow: isOpen ? '0 0 35px rgba(245, 158, 11, 0.08)' : 'none'
    }}>
      {/* Top Bar: Title & Action Controls */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '16px',
        borderBottom: '1px solid var(--border-subtle)',
        paddingBottom: '18px'
      }}>
        {/* Left: Identifiers & Financial Amount */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
            <span className="badge font-mono" style={{ background: 'rgba(255, 255, 255, 0.1)', color: '#ffffff', fontSize: '0.74rem', fontWeight: 800 }}>
              {incident.incidentId || incident.id}
            </span>
            <span className="badge font-mono" style={{ background: '#18181b', color: '#a1a1aa', border: '1px solid var(--border-subtle)', fontSize: '0.74rem' }}>
              {incident.paymentId}
            </span>
            <span className="badge badge-outline" style={{ fontSize: '0.7rem' }}>
              {incident.paymentMethod || 'UPI'}
            </span>
            <span className={`badge ${
              isResolved ? 'badge-success' : isOpen ? 'badge-high' : 'badge-critical'
            }`} style={{ fontSize: '0.7rem', fontWeight: 700 }}>
              {isResolved ? 'CASE RESOLVED' : isOpen ? 'CASE OPEN • AWAITING AI INVESTIGATION' : (incident.predictedRootCause || incident.incidentType)?.replace(/_/g, ' ')}
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'baseline', gap: '14px' }}>
            <div className="font-display" style={{ fontSize: '2.4rem', fontWeight: 800, color: '#ffffff', letterSpacing: '-0.03em' }}>
              ₹{amountFormatted}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{
                fontSize: '0.85rem',
                fontWeight: 700,
                color: isResolved ? '#10b981' : isOpen ? '#f59e0b' : '#ef4444'
              }}>
                {isResolved
                  ? 'STATUS: REMEDIATED (AUTO-REFUND EXECUTED)'
                  : isOpen
                  ? 'STATUS: OPEN DISCREPANCY DETECTED'
                  : 'STATUS: GHOST DEBIT DETECTED (STATE CLASH)'}
              </span>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                • {merchantName} ({orderId})
              </span>
            </div>
          </div>
        </div>

        {/* Right: Primary Action Trigger */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          {isOpen ? (
            <button
              onClick={onTriggerReanalysis}
              className="btn btn-sm animate-pulse"
              style={{
                background: 'linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%)',
                color: '#ffffff',
                fontWeight: 800,
                padding: '12px 24px',
                fontSize: '0.92rem',
                borderRadius: '10px',
                border: 'none',
                boxShadow: '0 0 20px rgba(6, 182, 212, 0.45)',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                cursor: 'pointer'
              }}
            >
              <Zap size={18} fill="#ffffff" />
              ⚡ RUN INVESTIGATION
            </button>
          ) : (
            <>
              <button
                onClick={onTriggerReanalysis}
                className="btn btn-outline-white btn-sm"
                style={{ padding: '8px 16px', fontSize: '0.82rem', borderRadius: '8px' }}
                title="Re-run Random Forest ML & Gemini Multi-Source Assessment"
              >
                <Sparkles size={14} style={{ color: '#06b6d4' }} />
                AI Re-Assess
              </button>
              <button
                onClick={onOpenResolution}
                className="btn btn-white btn-sm"
                style={{ padding: '8px 20px', fontSize: '0.84rem', fontWeight: 800, borderRadius: '8px', gap: '6px' }}
              >
                <ShieldCheck size={16} />
                Resolve Incident
              </button>
            </>
          )}
        </div>
      </div>

      {/* Unanalyzed Callout Banner if Open */}
      {isOpen && (
        <div style={{
          background: 'rgba(245, 158, 11, 0.08)',
          border: '1px solid rgba(245, 158, 11, 0.3)',
          borderRadius: '10px',
          padding: '16px 18px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '8px',
              background: 'rgba(245, 158, 11, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#f59e0b',
              flexShrink: 0
            }}>
              <AlertTriangle size={20} />
            </div>
            <div>
              <div style={{ fontSize: '0.88rem', fontWeight: 700, color: '#ffffff', marginBottom: '2px' }}>
                Unanalyzed Distributed Telemetry Discrepancy
              </div>
              <div style={{ fontSize: '0.78rem', color: '#d4d4d8', lineHeight: 1.45 }}>
                Bank Switch confirms ₹{amountFormatted} debited under UTR <strong className="font-mono">{bankUtr}</strong>, but Gateway reports TIMEOUT and Merchant OMS reports CANCELLED. Click <strong>"Run Investigation"</strong> to execute multi-agent forensic triage.
              </div>
            </div>
          </div>
          <button
            onClick={onTriggerReanalysis}
            className="btn btn-white btn-sm"
            style={{ padding: '8px 18px', fontSize: '0.82rem', fontWeight: 700, whiteSpace: 'nowrap', flexShrink: 0 }}
          >
            Investigate Incident →
          </button>
        </div>
      )}

      {/* 3 DISTINCT INVESTIGATION PANELS (Post-Investigation View) */}
      {!isOpen && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

          {/* ========================================================================= */}
          {/* PANEL A: AUTHORITATIVE SAFETY DECISION (from Java State Machine)          */}
          {/* ========================================================================= */}
          <div style={{
            background: 'rgba(239, 68, 68, 0.04)',
            border: '1px solid rgba(239, 68, 68, 0.35)',
            borderRadius: '12px',
            padding: '18px 20px',
            position: 'relative'
          }}>
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px', flexWrap: 'wrap', gap: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div style={{
                  width: '28px',
                  height: '28px',
                  borderRadius: '6px',
                  background: 'rgba(239, 68, 68, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#ef4444'
                }}>
                  <Lock size={16} />
                </div>
                <div>
                  <h3 className="font-display" style={{ fontSize: '0.95rem', fontWeight: 800, color: '#ffffff', letterSpacing: '-0.01em' }}>
                    AUTHORITATIVE SAFETY DECISION
                  </h3>
                  <div style={{ fontSize: '0.68rem', color: '#fca5a5', fontWeight: 600 }}>
                    Authority: Java Deterministic State Machine (Hard Financial Ground Truth)
                  </div>
                </div>
              </div>
              <span className="badge badge-critical font-mono" style={{ fontSize: '0.68rem', fontWeight: 700, padding: '4px 10px' }}>
                RETRY STRICTLY PROHIBITED
              </span>
            </div>

            {/* Metrics Grid */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px', marginBottom: '12px' }}>
              <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 14px' }}>
                <div style={{ fontSize: '0.66rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
                  Money At Risk
                </div>
                <div className="font-display font-mono" style={{ fontSize: '1.25rem', fontWeight: 800, color: '#ef4444' }}>
                  ₹{amountFormatted}
                </div>
                <div style={{ fontSize: '0.66rem', color: 'var(--text-muted)' }}>
                  Trapped at Nodal Switch
                </div>
              </div>

              <div style={{ background: '#050505', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: '8px', padding: '10px 14px' }}>
                <div style={{ fontSize: '0.66rem', fontWeight: 700, color: '#fca5a5', textTransform: 'uppercase', marginBottom: '4px' }}>
                  Customer Retry Status
                </div>
                <div className="font-display" style={{ fontSize: '1.05rem', fontWeight: 800, color: '#ef4444', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Lock size={15} /> BLOCKED
                </div>
                <div style={{ fontSize: '0.66rem', color: '#fca5a5' }}>
                  Safety Lock Enforced
                </div>
              </div>

              <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 14px' }}>
                <div style={{ fontSize: '0.66rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
                  Recommended Action
                </div>
                <div className="font-display" style={{ fontSize: '1.05rem', fontWeight: 800, color: '#10b981' }}>
                  AUTO REFUND
                </div>
                <div style={{ fontSize: '0.66rem', color: 'var(--text-muted)' }}>
                  Core Banking Reversal
                </div>
              </div>

              <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 14px' }}>
                <div style={{ fontSize: '0.66rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
                  Safe Next Step
                </div>
                <div className="font-display" style={{ fontSize: '0.88rem', fontWeight: 800, color: '#ffffff', marginTop: '2px' }}>
                  Instant UPI Credit
                </div>
                <div style={{ fontSize: '0.66rem', color: 'var(--text-muted)' }}>
                  Dispute window: &lt; 2h
                </div>
              </div>
            </div>

            {/* Why Retry Blocked Narrative */}
            <div style={{
              background: 'rgba(239, 68, 68, 0.08)',
              border: '1px solid rgba(239, 68, 68, 0.25)',
              borderRadius: '8px',
              padding: '10px 14px',
              fontSize: '0.82rem',
              color: '#ffffff',
              lineHeight: 1.45
            }}>
              <strong style={{ color: '#ef4444' }}>Safety Rule Invariant: </strong>
              Customer was already debited ₹{amountFormatted} (Bank UTR: <span className="font-mono" style={{ color: '#ffffff', fontWeight: 700 }}>{bankUtr}</span>). Retrying payment before refund or settlement will cause a duplicate debit on customer's account. Blind retry is strictly locked.
            </div>
          </div>

          {/* ========================================================================= */}
          {/* PANEL B: ML CLASSIFICATION (from Python FastAPI / Random Forest)          */}
          {/* ========================================================================= */}
          <div style={{
            background: 'rgba(6, 182, 212, 0.03)',
            border: '1px solid rgba(6, 182, 212, 0.3)',
            borderRadius: '12px',
            padding: '18px 20px'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', flexWrap: 'wrap', gap: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div style={{
                  width: '28px',
                  height: '28px',
                  borderRadius: '6px',
                  background: 'rgba(6, 182, 212, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#06b6d4'
                }}>
                  <Cpu size={16} />
                </div>
                <div>
                  <h3 className="font-display" style={{ fontSize: '0.95rem', fontWeight: 800, color: '#ffffff' }}>
                    ML ROOT CAUSE CLASSIFICATION
                  </h3>
                  <div style={{ fontSize: '0.68rem', color: '#a1a1aa' }}>
                    Engine: Random Forest Classifier (120 Trees) • Python FastAPI (/api/classify)
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span className="badge badge-cyan font-mono" style={{ fontSize: '0.68rem' }}>
                  ANOMALY SCORE: {anomalyScore}
                </span>
                <span className="badge" style={{ background: 'rgba(16, 185, 129, 0.15)', color: '#10b981', border: '1px solid rgba(16, 185, 129, 0.3)', fontSize: '0.74rem', fontWeight: 800 }}>
                  CONFIDENCE: {confidenceScore}%
                </span>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'minmax(220px, 1fr) 2fr', gap: '14px', alignItems: 'center' }}>
              {/* Classification */}
              <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '12px 14px' }}>
                <div style={{ fontSize: '0.66rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
                  Predicted Root Cause
                </div>
                <div className="font-display" style={{ fontSize: '1.05rem', fontWeight: 800, color: '#ffffff' }}>
                  {(incident.predictedRootCause || incident.incidentType)?.replace(/_/g, ' ')}
                </div>
                <div style={{ fontSize: '0.68rem', color: '#06b6d4', marginTop: '4px' }}>
                  14 multi-party continuous & categorical features evaluated
                </div>
              </div>

              {/* Contributing Telemetry Signals */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '8px' }}>
                <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '6px', padding: '6px 10px', fontSize: '0.74rem' }}>
                  <span style={{ color: 'var(--text-dim)' }}>bank_status: </span>
                  <span className="font-mono" style={{ color: '#10b981', fontWeight: 700 }}>SUCCESS / DEBITED (420ms)</span>
                </div>
                <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '6px', padding: '6px 10px', fontSize: '0.74rem' }}>
                  <span style={{ color: 'var(--text-dim)' }}>gateway_status: </span>
                  <span className="font-mono" style={{ color: '#ef4444', fontWeight: 700 }}>FAILED / TIMEOUT (65s)</span>
                </div>
                <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '6px', padding: '6px 10px', fontSize: '0.74rem' }}>
                  <span style={{ color: 'var(--text-dim)' }}>order_status: </span>
                  <span className="font-mono" style={{ color: '#ef4444', fontWeight: 700 }}>CANCELLED (Cart Released)</span>
                </div>
                <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '6px', padding: '6px 10px', fontSize: '0.74rem' }}>
                  <span style={{ color: 'var(--text-dim)' }}>webhook_status: </span>
                  <span className="font-mono" style={{ color: '#f59e0b', fontWeight: 700 }}>DROPPED (HTTP 504, 3 tries)</span>
                </div>
              </div>
            </div>
          </div>

          {/* ========================================================================= */}
          {/* PANEL C: GEMINI FORENSIC ASSISTANT (Plain-English Advisory Synthesis)     */}
          {/* ========================================================================= */}
          <div style={{
            background: 'rgba(168, 85, 247, 0.03)',
            border: '1px solid rgba(168, 85, 247, 0.3)',
            borderRadius: '12px',
            padding: '18px 20px'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', flexWrap: 'wrap', gap: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <div style={{
                  width: '28px',
                  height: '28px',
                  borderRadius: '6px',
                  background: 'rgba(168, 85, 247, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#a855f7'
                }}>
                  <Brain size={16} />
                </div>
                <div>
                  <h3 className="font-display" style={{ fontSize: '0.95rem', fontWeight: 800, color: '#ffffff' }}>
                    GEMINI FORENSIC ASSISTANT
                  </h3>
                  <div style={{ fontSize: '0.68rem', color: '#d8b4fe' }}>
                    Plain-English Telemetry Synthesis • Strictly Advisory
                  </div>
                </div>
              </div>
              <span className="badge" style={{ background: 'rgba(168, 85, 247, 0.12)', color: '#c084fc', border: '1px solid rgba(168, 85, 247, 0.3)', fontSize: '0.66rem' }}>
                ADVISORY REASONING
              </span>
            </div>

            {/* Narrative Explanation */}
            <div style={{
              background: '#060608',
              border: '1px solid var(--border-subtle)',
              borderRadius: '8px',
              padding: '14px 16px',
              fontSize: '0.88rem',
              color: '#ffffff',
              lineHeight: 1.6,
              marginBottom: '12px'
            }}>
              {ai.geminiExplanation?.whatHappened || ai.whatHappened || (
                `The customer initiated a ₹${amountFormatted} UPI payment for ${merchantName} order ${orderId}. The bank successfully debited the customer's account with UTR ${bankUtr} in 420ms. However, the payment gateway experienced a 65-second network timeout and marked authorization as FAILED. The merchant OMS did not receive confirmation before its checkout timer expired and cancelled the order. Furthermore, the webhook pipeline returned HTTP 504 across 3 delivery attempts, leaving the customer debited for an unfulfilled order.`
              )}
            </div>

            {/* Forensic Detail Points */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px' }}>
              <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 12px' }}>
                <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#c084fc', textTransform: 'uppercase', marginBottom: '4px' }}>
                  Key Evidence
                </div>
                <div style={{ fontSize: '0.74rem', color: '#d4d4d8', lineHeight: 1.4 }}>
                  Bank ISO-8583 approved (200 OK, UTR: {bankUtr}); Gateway timed out (65s); Merchant OMS cancelled.
                </div>
              </div>

              <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 12px' }}>
                <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#ef4444', textTransform: 'uppercase', marginBottom: '4px' }}>
                  Critical Contradiction
                </div>
                <div style={{ fontSize: '0.74rem', color: '#d4d4d8', lineHeight: 1.4 }}>
                  Bank reports SUCCESS (funds debited) vs Gateway reports FAILED and OMS cancelled cart.
                </div>
              </div>

              <div style={{ background: '#050505', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '10px 12px' }}>
                <div style={{ fontSize: '0.68rem', fontWeight: 700, color: '#10b981', textTransform: 'uppercase', marginBottom: '4px' }}>
                  Operator Action
                </div>
                <div style={{ fontSize: '0.74rem', color: '#d4d4d8', lineHeight: 1.4 }}>
                  Execute AUTO_REFUND_CUSTOMER to return ₹{amountFormatted} directly to original account.
                </div>
              </div>
            </div>
          </div>

        </div>
      )}

      {/* 5 Core Pillars in a Clean Balanced Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(5, 1fr)',
        gap: '10px',
        borderTop: '1px solid var(--border-subtle)',
        paddingTop: '14px'
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
            {isBankDebited ? `Debited ₹${amountFormatted}` : 'Not Debited'}
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
            {isGhostDebit ? 'GHOST DEBIT' : isOpen ? 'DISCREPANT' : 'CLASH'}
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
            {isOpen ? 'PENDING' : `${confidenceScore}%`}
          </div>
          <div style={{ fontSize: '0.68rem', color: 'var(--text-muted)' }}>
            Random Forest (120 Trees)
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
            {(ai.recommendedAction || incident.recommendedAction || 'AUTO_REFUND_CUSTOMER').replace(/_/g, ' ')}
          </div>
          <div style={{ fontSize: '0.68rem', color: incident.isRetryProhibited ? '#fca5a5' : '#86efac', fontWeight: 600 }}>
            {incident.isRetryProhibited ? 'Retry Prohibited' : 'Retry Permitted'}
          </div>
        </div>
      </div>
    </div>
  );
}
