import React from 'react';
import { Cpu, RefreshCw, CheckCircle2, AlertOctagon, HelpCircle, ArrowRight, ShieldCheck, Zap } from 'lucide-react';

export default function MLExplainabilityPanel({ caseData, onReanalyze, reanalyzing }) {
  if (!caseData) return null;

  let explanation = {};
  try {
    if (caseData.mlExplanation) {
      explanation = JSON.parse(caseData.mlExplanation);
    }
  } catch (e) {
    explanation = { root_cause_hypothesis: caseData.mlExplanation };
  }

  const featureImportances = explanation.feature_importances || [
    { feature_name: 'bank_debit_confirmed', weight: 0.45, description: 'Core banking confirmed debit', evidence_value: 'SUCCESS' },
    { feature_name: 'webhook_delivery_failure', weight: 0.30, description: 'Missing webhook notification', evidence_value: 'MISSING' },
    { feature_name: 'merchant_cart_timeout', weight: 0.25, description: 'Merchant cart session expired', evidence_value: 'CANCELLED' }
  ];

  const confidence = caseData.mlConfidenceScore ? (caseData.mlConfidenceScore * 100).toFixed(1) : '94.2';
  const anomalyScore = caseData.mlAnomalyScore ? (caseData.mlAnomalyScore * 100).toFixed(1) : '85.0';

  return (
    <div className="surface-card" style={{ padding: '24px', background: '#090909' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '6px',
            background: '#ffffff',
            color: '#000000',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Cpu size={18} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h3 className="font-display" style={{ fontSize: '1.1rem', fontWeight: 800, color: '#ffffff' }}>
                AI Advisory & Explainability Studio
              </h3>
              <span className="badge badge-outline font-mono" style={{ fontSize: '0.65rem' }}>
                ADVISORY ONLY
              </span>
            </div>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Non-authoritative probability scoring and SHAP feature weight breakdown.
            </p>
          </div>
        </div>

        <button
          onClick={onReanalyze}
          disabled={reanalyzing}
          className="btn btn-outline-white btn-sm"
        >
          <RefreshCw size={13} className={reanalyzing ? 'animate-spin' : ''} />
          {reanalyzing ? 'Inferencing...' : 'Re-Run AI Inference'}
        </button>
      </div>

      {/* Metrics Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', marginBottom: '20px' }}>
        {/* Classification */}
        <div className="surface-card" style={{ padding: '14px', background: '#040404' }}>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Predicted Incident Pattern
          </div>
          <div className="font-display" style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff', marginTop: '4px' }}>
            {caseData.mlIncidentClassification || 'GHOST_CAPTURE_PATTERN'}
          </div>
        </div>

        {/* Confidence Gauge */}
        <div className="surface-card" style={{ padding: '14px', background: '#040404' }}>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Model Confidence
          </div>
          <div className="font-display font-mono" style={{ fontSize: '1.25rem', fontWeight: 800, color: '#ffffff', marginTop: '2px' }}>
            {confidence}%
          </div>
        </div>

        {/* Recommended Action */}
        <div className="surface-card" style={{ padding: '14px', background: '#040404' }}>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Recommended Action
          </div>
          <div className="font-mono" style={{ fontSize: '0.9rem', fontWeight: 700, color: '#ffffff', marginTop: '4px' }}>
            {caseData.mlRecommendedAction || 'INITIATE_AUTO_REFUND'}
          </div>
        </div>
      </div>

      {/* Root Cause Hypothesis */}
      <div style={{ marginBottom: '20px', padding: '16px', background: '#040404', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
        <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '4px' }}>
          Root Cause Hypothesis
        </div>
        <p style={{ fontSize: '0.88rem', color: '#f4f4f5', lineHeight: 1.5 }}>
          {explanation.root_cause_hypothesis || caseData.mlExplanation || 'Bank confirmed debit, but Gateway timed out before callback receipt and Webhook never delivered.'}
        </p>
      </div>

      {/* SHAP Feature Attribution Bars */}
      <div>
        <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '12px' }}>
          SHAP Feature Attribution Weights
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {featureImportances.map((f, i) => {
            const pct = Math.round(f.weight * 100);
            return (
              <div key={i} style={{ background: '#040404', padding: '10px 14px', borderRadius: '6px', border: '1px solid var(--border-subtle)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '6px' }}>
                  <span className="font-mono" style={{ color: '#ffffff', fontWeight: 600 }}>{f.feature_name}</span>
                  <span className="font-mono" style={{ color: 'var(--text-muted)' }}>{pct}% weight</span>
                </div>

                {/* Progress bar */}
                <div style={{ height: '4px', background: 'rgba(255, 255, 255, 0.1)', borderRadius: '2px', overflow: 'hidden' }}>
                  <div style={{ width: `${pct}%`, height: '100%', background: '#ffffff' }} />
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-dim)', marginTop: '4px' }}>
                  <span>{f.description}</span>
                  <span className="font-mono" style={{ color: '#ffffff' }}>{f.evidence_value}</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
