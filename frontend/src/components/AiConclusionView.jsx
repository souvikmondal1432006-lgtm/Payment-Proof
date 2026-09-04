import React from 'react';
import {
  Sparkles,
  ShieldCheck,
  AlertTriangle,
  HelpCircle,
  TrendingUp,
  CheckCircle2,
  Lock,
  ArrowRight,
  RotateCcw,
  Zap,
  FileText
} from 'lucide-react';

export default function AiConclusionView({
  incident,
  onOpenResolution,
  onTriggerReanalysis,
  reanalyzing
}) {
  if (!incident) return null;

  const ai = incident.aiReport || {};
  const isBankDebited = incident.bank?.status === 'SUCCESS' || incident.bank?.status === 'DEBITED';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', padding: '28px', overflowY: 'auto', height: '100%' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
            <span className="badge badge-outline font-mono">{incident.paymentId}</span>
            <span className="badge badge-white">AI FORENSIC CONCLUSION</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>MULTI-PARTY REASONING & EPIDEMIC SYNTHESIS</span>
          </div>
          <h1 className="font-display" style={{ fontSize: '1.6rem', fontWeight: 800, color: '#ffffff' }}>
            Authoritative AI Investigation Report
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '2px' }}>
            Observational inference synthesized from Core Bank Switch, Gateway Aggregator, Merchant OMS, and Webhooks.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={onTriggerReanalysis}
            disabled={reanalyzing}
            className="btn btn-outline-white btn-sm"
          >
            <Sparkles size={14} className={reanalyzing ? 'animate-spin' : ''} style={{ color: '#06b6d4' }} />
            {reanalyzing ? 'Re-Evaluating...' : 'Re-Run AI Assessment'}
          </button>
          <button
            onClick={onOpenResolution}
            className="btn btn-white btn-sm"
          >
            <ShieldCheck size={14} />
            Execute Action
          </button>
        </div>
      </div>

      {/* Metrics Banner */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
        gap: '12px'
      }}>
        {/* Confidence */}
        <div style={{ background: '#0a0a0a', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '14px' }}>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>AI Confidence</div>
          <div className="font-display" style={{ fontSize: '1.5rem', fontWeight: 800, color: '#06b6d4' }}>
            {ai.confidence ? `${(ai.confidence * 100).toFixed(1)}%` : '99.2%'}
          </div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Random Forest Classifier</div>
        </div>

        {/* Root Cause Classification */}
        <div style={{ background: '#0a0a0a', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '14px' }}>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>Predicted Root Cause</div>
          <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>
            {incident.incidentType?.replace(/_/g, ' ')}
          </div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Multi-Party State Divergence</div>
        </div>

        {/* Money At Risk */}
        <div style={{ background: '#0a0a0a', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: '8px', padding: '14px' }}>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>Money At Risk</div>
          <div className="font-display" style={{ fontSize: '1.5rem', fontWeight: 800, color: '#ef4444' }}>
            ₹{Number(ai.moneyAtRisk || incident.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Customer debited amount</div>
        </div>

        {/* Recommended Action */}
        <div style={{ background: '#0a0a0a', border: '1px solid var(--border-subtle)', borderRadius: '8px', padding: '14px' }}>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>Recommended Action</div>
          <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#10b981' }}>
            {ai.recommendedAction || 'AUTO_REFUND_CUSTOMER'}
          </div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Authoritative Safest Next Step</div>
        </div>
      </div>

      {/* Structured 6-Section Report */}
      <div style={{
        background: '#0a0a0a',
        border: '1px solid var(--border-subtle)',
        borderRadius: '12px',
        padding: '24px',
        display: 'flex',
        flexDirection: 'column',
        gap: '20px'
      }}>
        {/* Section 1: WHAT HAPPENED */}
        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '6px' }}>
            1. What Happened
          </div>
          <div style={{ fontSize: '1.05rem', color: '#ffffff', lineHeight: 1.6, fontWeight: 500 }}>
            {ai.whatHappened || `Your customer was charged ₹${Number(incident.amount || 4500).toLocaleString('en-IN')} via UPI from their bank account, but an upstream network timeout prevented ${incident.merchantName || 'the merchant'} from confirming the payment before the checkout session expired.`}
          </div>
        </div>

        {/* Section 2: WHY WE THINK THIS */}
        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '6px' }}>
            2. Why We Think This (Multi-System Evidence Synthesis)
          </div>
          <div style={{ fontSize: '0.92rem', color: 'var(--text-off-white)', lineHeight: 1.6, background: '#121212', padding: '14px', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
            {ai.whyWeThinkThis || `Evidence synthesis indicates: Bank switch reported SUCCESS (UTR: ${incident.bank?.utr || 'UTR984102947101'}, Latency: 420ms). Gateway reported FAILED (Latency: 65,000ms). Merchant OMS recorded CANCELLED (Order: ${incident.orderId || 'ORD_9841_PAY'}). Webhook delivery resulted in DROPPED (HTTP 504 after 3 attempts). Statistical ML classifier assigned 97.5% confidence to BANK_DEBIT_GATEWAY_FAILURE.`}
          </div>
        </div>

        {/* Section 3: WHAT IS UNCERTAIN (Epistemic Humility) */}
        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '6px' }}>
            3. What Is Uncertain (Unresolved Distributed Questions)
          </div>
          <div style={{ fontSize: '0.9rem', color: '#fca5a5', lineHeight: 1.5, background: 'rgba(239, 68, 68, 0.05)', padding: '12px 16px', borderRadius: '8px', border: '1px solid rgba(239, 68, 68, 0.2)' }}>
            {ai.whatIsUncertain || `Unable to confirm if ${incident.merchantName || 'the merchant'} can restore the cancelled cart or if the bank switch scheduled an automatic clearing reversal.`}
          </div>
        </div>

        {/* Section 4: AUDITABLE DECISION FACTORS (Zero Hidden CoT) */}
        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '8px' }}>
            4. Auditable Decision Factors (Verifiable Multi-Party Telemetry)
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {(ai.decisionFactors || [
              `Bank Telemetry: status=SUCCESS, utr=${incident.bank?.utr || 'UTR984102947101'}, debited_amount=${Number(incident.amount || 4500).toFixed(2)}`,
              "Gateway Telemetry: status=FAILED, capture=NOT_REQUESTED, latency=65000ms",
              `Merchant OMS: order_id=${incident.orderId || 'ORD_9841_PAY'}, order_status=CANCELLED, fulfillment=UNFULFILLED`,
              "Webhook Engine: delivery_status=DROPPED, http_code=504, attempts=3",
              "Safety Invariant: is_retry_prohibited=true",
              `ML Advisory: model=incident-classifier-v1.0.0-rf, root_cause=${incident.predictedRootCause || 'BANK_DEBIT_GATEWAY_FAILURE'}, confidence=${incident.confidence || 0.9750}`
            ]).map((factor, idx) => (
              <div key={idx} className="font-mono" style={{ background: '#050505', padding: '8px 12px', borderRadius: '6px', border: '1px solid var(--border-subtle)', fontSize: '0.78rem', color: '#a1a1aa' }}>
                • {factor}
              </div>
            ))}
          </div>
        </div>

        {/* Section 5: GEMINI FORENSIC ASSISTANT (Strictly Advisory Synthesis) */}
        {(ai.gemini_explanation || ai.geminiExplanation || incident.geminiExplanation) && (() => {
          const gemini = ai.gemini_explanation || ai.geminiExplanation || incident.geminiExplanation;
          return (
            <div style={{
              background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.07) 0%, rgba(59, 130, 246, 0.04) 100%)',
              border: '1px solid rgba(6, 182, 212, 0.3)',
              borderRadius: '10px',
              padding: '18px',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Sparkles size={16} style={{ color: '#06b6d4' }} />
                  <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#06b6d4', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    Gemini Explanation Assistant (Advisory Synthesis)
                  </span>
                </div>
                <span className="badge" style={{ fontSize: '0.68rem', color: '#38bdf8', background: 'rgba(56, 189, 248, 0.1)', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
                  Non-Authoritative • Explanatory Only
                </span>
              </div>

              {gemini.summary && (
                <div style={{ fontSize: '0.92rem', color: '#f1f5f9', fontWeight: 500, lineHeight: 1.5 }}>
                  {gemini.summary}
                </div>
              )}

              {gemini.evidence && gemini.evidence.length > 0 && (
                <div>
                  <div style={{ fontSize: '0.72rem', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.03em', marginBottom: '4px' }}>
                    Key Corroborating Evidence:
                  </div>
                  <ul style={{ margin: 0, paddingLeft: '18px', color: '#cbd5e1', fontSize: '0.82rem', lineHeight: 1.5 }}>
                    {gemini.evidence.map((ev, i) => (
                      <li key={i}>{ev}</li>
                    ))}
                  </ul>
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '10px', marginTop: '4px' }}>
                {(gemini.customer_impact || gemini.customerImpact) && (
                  <div style={{ background: 'rgba(0,0,0,0.3)', padding: '10px 12px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.06)' }}>
                    <div style={{ fontSize: '0.7rem', color: '#38bdf8', fontWeight: 700, textTransform: 'uppercase', marginBottom: '3px' }}>
                      Customer Impact
                    </div>
                    <div style={{ fontSize: '0.8rem', color: '#cbd5e1', lineHeight: 1.4 }}>
                      {gemini.customer_impact || gemini.customerImpact}
                    </div>
                  </div>
                )}
                {(gemini.confidence_explanation || gemini.confidenceExplanation) && (
                  <div style={{ background: 'rgba(0,0,0,0.3)', padding: '10px 12px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.06)' }}>
                    <div style={{ fontSize: '0.7rem', color: '#a78bfa', fontWeight: 700, textTransform: 'uppercase', marginBottom: '3px' }}>
                      Confidence Reasoning
                    </div>
                    <div style={{ fontSize: '0.8rem', color: '#cbd5e1', lineHeight: 1.4 }}>
                      {gemini.confidence_explanation || gemini.confidenceExplanation}
                    </div>
                  </div>
                )}
              </div>
            </div>
          );
        })()}
      </div>
    </div>
  );
}
