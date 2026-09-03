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
              AI Forensic Model
            </span>
            <Sparkles size={18} style={{ color: '#06b6d4' }} />
          </div>
          <div className="font-display" style={{ fontSize: '2rem', fontWeight: 800, color: '#06b6d4' }}>
            99.8%
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            Random Forest (120 Estimators)
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
            Showing {cases.length} active cases
          </span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {cases.map((c) => (
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
