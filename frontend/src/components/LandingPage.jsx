import React from 'react';
import { ArrowRight, ShieldCheck, Zap, Lock, Activity, Layers, Terminal, Sparkles, CheckCircle2, ChevronRight, Cpu, Eye } from 'lucide-react';

export default function LandingPage({ onLaunchApp, onQuickLogin }) {
  return (
    <div style={{ minHeight: '100vh', background: '#000000', color: '#ffffff', overflowX: 'hidden' }}>
      {/* Navigation */}
      <nav style={{
        height: '72px',
        borderBottom: '1px solid var(--border-subtle)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 40px',
        maxWidth: '1400px',
        margin: '0 auto'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '36px',
            height: '36px',
            background: '#ffffff',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#000000'
          }}>
            <ShieldCheck size={22} strokeWidth={2.5} />
          </div>
          <div>
            <span className="font-display" style={{ fontSize: '1.15rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
              PAYMENT PROOF
            </span>
            <span style={{ fontSize: '0.7rem', color: 'var(--text-dim)', marginLeft: '8px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Core Engine
            </span>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          <button
            onClick={() => onQuickLogin('operator_priya_m')}
            className="btn btn-outline-white btn-sm"
          >
            Demo Operator Access
          </button>
          <button
            onClick={onLaunchApp}
            className="btn btn-white btn-sm"
          >
            Launch Console <ArrowRight size={14} />
          </button>
        </div>
      </nav>

      {/* Hero Section */}
      <main style={{ maxWidth: '1300px', margin: '0 auto', padding: '80px 40px 120px' }}>
        {/* Top Tagline */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div className="badge badge-outline" style={{ padding: '6px 14px', borderRadius: '9999px', fontSize: '0.75rem' }}>
            <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#10b981', display: 'inline-block' }} />
            AUTHORITATIVE MULTI-PARTY FORENSICS • PROD 1.0
          </div>
        </div>

        {/* Hero Title */}
        <div style={{ textAlign: 'center', maxWidth: '960px', margin: '0 auto 28px' }}>
          <h1 className="font-display" style={{
            fontSize: 'clamp(2.8rem, 6vw, 4.5rem)',
            fontWeight: 800,
            lineHeight: 1.05,
            letterSpacing: '-0.04em',
            color: '#ffffff'
          }}>
            Zero Inconsistency in Digital Payments.
          </h1>
          <p style={{
            fontSize: 'clamp(1.05rem, 2vw, 1.25rem)',
            color: 'var(--text-muted)',
            marginTop: '24px',
            lineHeight: 1.6,
            maxWidth: '780px',
            margin: '24px auto 0'
          }}>
            When Banks confirm debits, Gateways time out, and Merchant carts cancel, <strong style={{ color: '#ffffff' }}>Payment Proof</strong> reconciles multi-party state divergence with explainable AI forensics and immutable audit integrity.
          </p>
        </div>

        {/* CTA Buttons */}
        <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', marginTop: '40px', flexWrap: 'wrap' }}>
          <button
            onClick={onLaunchApp}
            className="btn btn-white"
            style={{ padding: '14px 32px', fontSize: '1rem', fontWeight: 700 }}
          >
            Enter Investigation Studio <ArrowRight size={18} />
          </button>

          <button
            onClick={() => onQuickLogin('operator_arjun_k')}
            className="btn btn-outline-white"
            style={{ padding: '14px 28px', fontSize: '1rem' }}
          >
            <Zap size={18} style={{ color: '#f59e0b' }} />
            Sign In with Security Clearance
          </button>
        </div>

        {/* Live Interactive Interactive Preview */}
        <div style={{ marginTop: '80px', position: 'relative' }}>
          <div style={{
            position: 'absolute',
            inset: '-1px',
            background: 'linear-gradient(180deg, rgba(255,255,255,0.2) 0%, transparent 60%)',
            borderRadius: '16px',
            zIndex: 0
          }} />

          <div className="surface-card" style={{
            position: 'relative',
            zIndex: 1,
            padding: '32px',
            background: '#080808',
            borderRadius: '16px',
            border: '1px solid var(--border-subtle)'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <span className="badge badge-critical">GHOST CAPTURE DETECTED</span>
                <span className="font-mono" style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>TX_99810A • INR 4,499.00</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.8rem', color: '#10b981' }}>
                <CheckCircle2 size={16} />
                <span>AI Confidence: 98.4%</span>
              </div>
            </div>

            {/* Visual Timeline Nodes */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
              <div style={{ padding: '16px', background: '#0f0f0f', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: '10px' }}>
                <div style={{ fontSize: '0.72rem', color: '#10b981', fontWeight: 700, textTransform: 'uppercase' }}>1. Bank Debit Ledger</div>
                <div className="font-display" style={{ fontSize: '1.1rem', fontWeight: 800, marginTop: '4px' }}>SUCCESS (00)</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '4px' }}>HDFC Core Banking debited A/C XXXX1290</div>
              </div>

              <div style={{ padding: '16px', background: '#0f0f0f', border: '1px solid rgba(245, 158, 11, 0.3)', borderRadius: '10px' }}>
                <div style={{ fontSize: '0.72rem', color: '#f59e0b', fontWeight: 700, textTransform: 'uppercase' }}>2. Gateway Processing</div>
                <div className="font-display" style={{ fontSize: '1.1rem', fontWeight: 800, marginTop: '4px' }}>PENDING / TIMEOUT</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '4px' }}>Socket timeout waiting for PSP callback</div>
              </div>

              <div style={{ padding: '16px', background: '#0f0f0f', border: '1px solid rgba(239, 68, 68, 0.3)', borderRadius: '10px' }}>
                <div style={{ fontSize: '0.72rem', color: '#ef4444', fontWeight: 700, textTransform: 'uppercase' }}>3. Merchant OMS</div>
                <div className="font-display" style={{ fontSize: '1.1rem', fontWeight: 800, marginTop: '4px' }}>CANCELLED</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '4px' }}>Checkout cart expired with zero proof</div>
              </div>

              <div style={{ padding: '16px', background: '#0f0f0f', border: '1px solid rgba(168, 85, 247, 0.3)', borderRadius: '10px' }}>
                <div style={{ fontSize: '0.72rem', color: '#a78bfa', fontWeight: 700, textTransform: 'uppercase' }}>4. AI Advisory Resolution</div>
                <div className="font-display" style={{ fontSize: '1.1rem', fontWeight: 800, marginTop: '4px', color: '#ffffff' }}>AUTO-REFUND</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '4px' }}>SHAP attribution weight 0.984 (Certain)</div>
              </div>
            </div>
          </div>
        </div>

        {/* Feature Grid */}
        <div style={{ marginTop: '100px', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '24px' }}>
          <div className="surface-interactive" style={{ padding: '28px' }}>
            <div style={{ width: '40px', height: '40px', background: 'rgba(255,255,255,0.06)', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '16px' }}>
              <Zap size={20} style={{ color: '#ffffff' }} />
            </div>
            <h3 className="font-display" style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '8px' }}>Ghost Capture Elimination</h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
              Detects dropped webhooks and network partition anomalies before customer support escalations or chargeback penalties.
            </p>
          </div>

          <div className="surface-interactive" style={{ padding: '28px' }}>
            <div style={{ width: '40px', height: '40px', background: 'rgba(255,255,255,0.06)', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '16px' }}>
              <Cpu size={20} style={{ color: '#ffffff' }} />
            </div>
            <h3 className="font-display" style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '8px' }}>Strictly Advisory AI</h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
              Calculates probabilities and SHAP feature weights without granting mutation or write privileges to non-deterministic models.
            </p>
          </div>

          <div className="surface-interactive" style={{ padding: '28px' }}>
            <div style={{ width: '40px', height: '40px', background: 'rgba(255,255,255,0.06)', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '16px' }}>
              <Lock size={20} style={{ color: '#ffffff' }} />
            </div>
            <h3 className="font-display" style={{ fontSize: '1.2rem', fontWeight: 700, marginBottom: '8px' }}>Immutable Audit Ledger</h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
              Every authoritative mutation, ML inference, and operator resolution is recorded into a tamper-evident append-only ledger.
            </p>
          </div>
        </div>

        {/* Bottom Banner */}
        <div style={{
          marginTop: '80px',
          padding: '48px',
          background: 'linear-gradient(180deg, #0c0c0c 0%, #050505 100%)',
          borderRadius: '16px',
          border: '1px solid var(--border-subtle)',
          textAlign: 'center'
        }}>
          <h2 className="font-display" style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '12px' }}>
            Ready to Inspect Live Multi-Party Telemetry?
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '24px', maxWidth: '600px', margin: '0 auto 24px' }}>
            Explore real-world UPI, Card, and NetBanking contradiction scenarios with live state transitions.
          </p>
          <button
            onClick={onLaunchApp}
            className="btn btn-white"
            style={{ padding: '14px 36px', fontSize: '1rem', fontWeight: 700 }}
          >
            Launch Investigation Platform
          </button>
        </div>
      </main>
    </div>
  );
}
