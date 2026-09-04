import React from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  TrendingUp,
  AlertTriangle,
  Clock,
  ArrowRight,
  Sparkles,
  Layers,
  Activity,
  CheckCircle2,
  Lock,
  Search
} from 'lucide-react';

export default function CommandCenterView({
  stats,
  cases,
  onSelectCase,
  onNavigateToSearch,
  onNavigateToHealth
}) {
  const criticalCases = cases.filter(c => c.severity === 'CRITICAL');
  const highCases = cases.filter(c => c.severity === 'HIGH');
  const totalMoneyAtRisk = cases.reduce((acc, c) => acc + (c.moneyAtRisk || 0), 0);

  // Ensure Hero Pitch Scenario (inc_test_001) is pinned at the top
  const sortedCases = [...cases].sort((a, b) => {
    if ((a.incidentId || a.id) === 'inc_test_001') return -1;
    if ((b.incidentId || b.id) === 'inc_test_001') return 1;
    return 0;
  });

  const heroCase = cases.find(c => (c.incidentId || c.id) === 'inc_test_001') || {
    incidentId: 'inc_test_001',
    paymentId: 'pay_test_001',
    amount: 4500.0,
    paymentMethod: 'UPI',
    severity: 'CRITICAL',
    caseStatus: 'OPEN',
    title: 'Critical Ghost Debit Detected on ORD-2026-TEST01'
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px', padding: '28px', overflowY: 'auto', height: '100%' }}>
      {/* Hero Mission Control Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        flexWrap: 'wrap',
        gap: '16px',
        borderBottom: '1px solid var(--border-subtle)',
        paddingBottom: '20px'
      }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
            <span className="badge badge-white">COMMAND CENTER</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>MISSION CONTROL & TRIAGE RADAR</span>
          </div>
          <h1 className="font-display" style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', letterSpacing: '-0.02em' }}>
            Payment Inconsistency Command Center
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '4px' }}>
            Real-time surveillance for multi-party payment state divergence, ghost debits, and dropped webhooks.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={onNavigateToSearch}
            className="btn btn-outline-white btn-sm"
          >
            <Search size={14} />
            Global Search
          </button>
          <button
            onClick={onNavigateToHealth}
            className="btn btn-outline-white btn-sm"
          >
            <Activity size={14} style={{ color: '#10b981' }} />
            System Health
          </button>
        </div>
      </div>

      {/* HERO PITCH DEMO HIGHLIGHT CARD */}
      <div style={{
        background: 'linear-gradient(135deg, rgba(239, 68, 68, 0.12), rgba(0, 0, 0, 0.8))',
        border: '1px solid rgba(239, 68, 68, 0.45)',
        boxShadow: '0 8px 30px rgba(239, 68, 68, 0.15)',
        borderRadius: '14px',
        padding: '20px 24px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '18px'
      }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxWidth: '680px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
            <span className="badge" style={{ background: '#ef4444', color: '#ffffff', fontWeight: 800, fontSize: '0.7rem' }}>
              HERO PITCH SCENARIO
            </span>
            <span className="badge font-mono" style={{ background: 'rgba(255,255,255,0.1)', color: '#ffffff', fontSize: '0.72rem' }}>
              inc_test_001 • pay_test_001
            </span>
            <span className="badge badge-outline" style={{ fontSize: '0.68rem', color: '#10b981', borderColor: '#10b981' }}>
              BANK: DEBITED (UTR984102947101)
            </span>
            <span className="badge badge-outline" style={{ fontSize: '0.68rem', color: '#ef4444', borderColor: '#ef4444' }}>
              GATEWAY: FAILED (TIMEOUT)
            </span>
            <span className="badge badge-outline" style={{ fontSize: '0.68rem', color: '#ef4444', borderColor: '#ef4444' }}>
              ORDER: CANCELLED
            </span>
            <span className="badge badge-outline" style={{ fontSize: '0.68rem', color: '#f59e0b', borderColor: '#f59e0b' }}>
              WEBHOOK: DROPPED (504)
            </span>
          </div>

          <div className="font-display" style={{ fontSize: '1.25rem', fontWeight: 800, color: '#ffffff' }}>
            Critical Ghost Debit: Customer debited ₹4,500 via UPI, but Gateway failed before capture
          </div>

          <div style={{ fontSize: '0.8rem', color: '#d4d4d8', lineHeight: 1.45 }}>
            Bank confirmed successful debit with valid UTR, but Razorpay timed out post-debit (65,000ms), Swiggy OMS released the cart reservation, and the webhook was dropped. Customer funds are actively locked.
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '0.75rem', fontWeight: 700, marginTop: '2px' }}>
            <span style={{ color: '#ef4444' }}>● MONEY AT RISK: ₹4,500.00</span>
            <span style={{ color: '#fca5a5' }}>● RETRY: BLOCKED BY JAVA RULES</span>
            <span style={{ color: '#06b6d4' }}>● RECOMMENDED: AUTO REFUND CUSTOMER</span>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '10px' }}>
          <div className="font-display" style={{ fontSize: '2.4rem', fontWeight: 900, color: '#ffffff' }}>
            ₹4,500<span style={{ fontSize: '1.2rem', color: 'var(--text-muted)' }}>.00</span>
          </div>
          <button
            onClick={() => onSelectCase('inc_test_001')}
            className="btn btn-white"
            style={{ padding: '10px 22px', fontSize: '0.88rem', fontWeight: 800, gap: '8px', boxShadow: '0 4px 15px rgba(255,255,255,0.2)' }}
          >
            Investigate Hero Incident <ArrowRight size={15} />
          </button>
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: '16px'
      }}>
        {/* Money At Risk */}
        <div style={{
          background: '#0a0a0a',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          borderRadius: '12px',
          padding: '20px',
          position: 'relative'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Total Money At Risk
            </span>
            <ShieldAlert size={18} style={{ color: '#ef4444' }} />
          </div>
          <div className="font-display" style={{ fontSize: '2rem', fontWeight: 800, color: '#ef4444' }}>
            ₹{totalMoneyAtRisk.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            Across {criticalCases.length} active ghost debit incidents
          </div>
        </div>

        {/* Active Incidents */}
        <div style={{
          background: '#0a0a0a',
          border: '1px solid var(--border-subtle)',
          borderRadius: '12px',
          padding: '20px'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Open Inconsistencies
            </span>
            <AlertTriangle size={18} style={{ color: '#f59e0b' }} />
          </div>
          <div className="font-display" style={{ fontSize: '2rem', fontWeight: 800, color: '#ffffff' }}>
            {cases.length}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            {criticalCases.length} Critical · {highCases.length} High Priority
          </div>
        </div>

        {/* Total Monitored Volume */}
        <div style={{
          background: '#0a0a0a',
          border: '1px solid var(--border-subtle)',
          borderRadius: '12px',
          padding: '20px'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Total Monitored Vol
            </span>
            <TrendingUp size={18} style={{ color: '#10b981' }} />
          </div>
          <div className="font-display" style={{ fontSize: '2rem', fontWeight: 800, color: '#ffffff' }}>
            ₹14.85L
          </div>
          <div style={{ fontSize: '0.75rem', color: '#10b981', marginTop: '4px' }}>
            99.98% clean settlement flow
          </div>
        </div>

        {/* ML Advisory Confidence */}
        <div style={{
          background: '#0a0a0a',
          border: '1px solid var(--border-subtle)',
          borderRadius: '12px',
          padding: '20px'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              ML Forensic Model
            </span>
            <Sparkles size={18} style={{ color: '#06b6d4' }} />
          </div>
          <div className="font-display" style={{ fontSize: '2rem', fontWeight: 800, color: '#06b6d4' }}>
            Random Forest
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            120 Estimators · Multi-Class Classification
          </div>
        </div>
      </div>

      {/* Incident Triage Queue */}
      <div style={{
        background: '#0a0a0a',
        border: '1px solid var(--border-subtle)',
        borderRadius: '12px',
        padding: '20px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div className="font-display" style={{ fontSize: '1.05rem', fontWeight: 700, color: '#ffffff' }}>
            Active Desynchronization Incidents Requiring Triage
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
            Showing {sortedCases.length} active cases
          </span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {sortedCases.map((c) => (
            <div
              key={c.incidentId}
              onClick={() => onSelectCase(c.incidentId)}
              style={{
                background: '#121212',
                border: '1px solid var(--border-subtle)',
                borderRadius: '8px',
                padding: '16px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                cursor: 'pointer',
                transition: 'all 0.2s'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = 'rgba(255,255,255,0.3)';
                e.currentTarget.style.background = '#18181b';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = 'var(--border-subtle)';
                e.currentTarget.style.background = '#121212';
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                <div style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '8px',
                  background: c.severity === 'CRITICAL' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: c.severity === 'CRITICAL' ? '#ef4444' : '#f59e0b'
                }}>
                  {c.severity === 'CRITICAL' ? <ShieldAlert size={18} /> : <AlertTriangle size={18} />}
                </div>

                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
                    <span className="font-mono" style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      {c.paymentId}
                    </span>
                    <span style={{ fontSize: '0.9rem', fontWeight: 700, color: '#ffffff' }}>
                      {c.title}
                    </span>
                    <span className={`badge ${c.severity === 'CRITICAL' ? 'badge-critical' : 'badge-high'}`}>
                      {c.severity}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                    Merchant: <span style={{ color: '#ffffff' }}>{c.merchantName || c.merchantId}</span> · Method: {c.paymentMethod}
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
                <div style={{ textAlign: 'right' }}>
                  <div className="font-display" style={{ fontSize: '1.1rem', fontWeight: 800, color: '#ffffff' }}>
                    ₹{Number(c.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>
                    {c.caseStatus}
                  </div>
                </div>

                <div className="btn btn-outline-white btn-sm" style={{ padding: '6px 12px' }}>
                  Investigate <ArrowRight size={13} />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
