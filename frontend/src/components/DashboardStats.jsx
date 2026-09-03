import React from 'react';
import { AlertOctagon, CheckCircle2, DollarSign, Brain, Layers } from 'lucide-react';

export default function DashboardStats({ stats }) {
  if (!stats) return null;

  const formatCurrency = (val) => {
    if (!val) return '₹0.00';
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(val);
  };

  const formatPercent = (val) => {
    if (!val) return '0%';
    return (val * 100).toFixed(1) + '%';
  };

  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
      gap: '12px',
      padding: '16px 24px',
      borderBottom: '1px solid var(--border-subtle)',
      background: '#040404'
    }}>
      {/* Total Incidents */}
      <div className="surface-card" style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: '14px', background: '#090909' }}>
        <div style={{ background: 'rgba(255, 255, 255, 0.06)', color: '#ffffff', padding: '10px', borderRadius: '8px' }}>
          <Layers size={18} />
        </div>
        <div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Total Incidents
          </div>
          <div className="font-display" style={{ fontSize: '1.4rem', fontWeight: 800, color: '#ffffff', marginTop: '2px' }}>
            {stats.totalCases || 0}
          </div>
        </div>
      </div>

      {/* Critical Severity */}
      <div className="surface-card" style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: '14px', background: '#090909' }}>
        <div style={{ background: 'rgba(239, 68, 68, 0.12)', color: '#ef4444', padding: '10px', borderRadius: '8px' }}>
          <AlertOctagon size={18} />
        </div>
        <div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Critical Severity
          </div>
          <div className="font-display" style={{ fontSize: '1.4rem', fontWeight: 800, color: '#ef4444', marginTop: '2px' }}>
            {stats.criticalSeverityCases || 0}
          </div>
        </div>
      </div>

      {/* Funds at Risk */}
      <div className="surface-card" style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: '14px', background: '#090909' }}>
        <div style={{ background: 'rgba(245, 158, 11, 0.12)', color: '#f59e0b', padding: '10px', borderRadius: '8px' }}>
          <DollarSign size={18} />
        </div>
        <div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Funds at Risk
          </div>
          <div className="font-display font-mono" style={{ fontSize: '1.4rem', fontWeight: 800, color: '#ffffff', marginTop: '2px' }}>
            {formatCurrency(stats.totalAmountAtRisk)}
          </div>
        </div>
      </div>

      {/* AI Advisory Conf */}
      <div className="surface-card" style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: '14px', background: '#090909' }}>
        <div style={{ background: 'rgba(139, 92, 246, 0.12)', color: '#a78bfa', padding: '10px', borderRadius: '8px' }}>
          <Brain size={18} />
        </div>
        <div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            AI Advisory Conf.
          </div>
          <div className="font-display font-mono" style={{ fontSize: '1.4rem', fontWeight: 800, color: '#ffffff', marginTop: '2px' }}>
            {formatPercent(stats.averageConfidenceScore)}
          </div>
        </div>
      </div>

      {/* Authoritative Resolved */}
      <div className="surface-card" style={{ padding: '14px 16px', display: 'flex', alignItems: 'center', gap: '14px', background: '#090909' }}>
        <div style={{ background: 'rgba(16, 185, 129, 0.12)', color: '#10b981', padding: '10px', borderRadius: '8px' }}>
          <CheckCircle2 size={18} />
        </div>
        <div>
          <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Reconciled Cases
          </div>
          <div className="font-display" style={{ fontSize: '1.4rem', fontWeight: 800, color: '#10b981', marginTop: '2px' }}>
            {stats.resolvedCases || 0}
          </div>
        </div>
      </div>
    </div>
  );
}
