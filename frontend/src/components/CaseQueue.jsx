import React, { useState } from 'react';
import { Search, AlertTriangle, CheckCircle, ArrowRight, Filter, ShieldAlert } from 'lucide-react';
import LoadingSkeleton from './LoadingSkeleton';
import EmptyState from './EmptyState';

export default function CaseQueue({ cases, selectedCaseId, onSelectCase, loading, onRetry }) {
  const [search, setSearch] = useState('');
  const [severityFilter, setSeverityFilter] = useState('ALL');

  const filteredCases = (cases || []).filter(c => {
    const q = search.toLowerCase();
    const matchesSearch = !q ||
      c.incidentId?.toLowerCase().includes(q) ||
      c.paymentId?.toLowerCase().includes(q) ||
      c.orderId?.toLowerCase().includes(q) ||
      c.merchantName?.toLowerCase().includes(q) ||
      c.merchantId?.toLowerCase().includes(q) ||
      c.customerName?.toLowerCase().includes(q) ||
      c.bank?.utr?.toLowerCase().includes(q);

    const matchesSeverity = severityFilter === 'ALL' || c.severity === severityFilter;
    return matchesSearch && matchesSeverity;
  });

  return (
    <div style={{
      width: '100%',
      display: 'flex',
      flexDirection: 'column',
      height: '100%',
      background: '#040404'
    }}>
      {/* Search & Filter Bar */}
      <div style={{ padding: '16px', borderBottom: '1px solid var(--border-subtle)' }}>
        <div style={{ position: 'relative', marginBottom: '10px' }}>
          <Search size={15} style={{ position: 'absolute', left: '12px', top: '11px', color: 'var(--text-dim)' }} />
          <input
            type="text"
            placeholder="Search TxID, UTR, Order, Merchant..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: '100%',
              padding: '9px 12px 9px 36px',
              background: '#000000',
              border: '1px solid var(--border-subtle)',
              borderRadius: '8px',
              color: '#ffffff',
              fontSize: '0.85rem',
              outline: 'none'
            }}
          />
        </div>

        {/* Severity Chips */}
        <div style={{ display: 'flex', gap: '6px' }}>
          {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM'].map(sev => (
            <button
              key={sev}
              onClick={() => setSeverityFilter(sev)}
              style={{
                background: severityFilter === sev ? '#ffffff' : 'rgba(255, 255, 255, 0.04)',
                border: `1px solid ${severityFilter === sev ? '#ffffff' : 'var(--border-subtle)'}`,
                color: severityFilter === sev ? '#000000' : 'var(--text-muted)',
                borderRadius: '6px',
                padding: '4px 10px',
                fontSize: '0.72rem',
                fontWeight: 700,
                cursor: 'pointer'
              }}
            >
              {sev}
            </button>
          ))}
        </div>
      </div>

      {/* Case List Body */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '12px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {loading ? (
          <LoadingSkeleton type="row" count={6} />
        ) : filteredCases.length > 0 ? (
          filteredCases.map(c => {
            const isSelected = selectedCaseId === c.incidentId || selectedCaseId === c.id;
            const isGhostDebit = c.incidentType === 'BANK_DEBIT_GATEWAY_FAILURE';

            return (
              <div
                key={c.incidentId || c.id}
                onClick={() => onSelectCase(c.incidentId || c.id)}
                style={{
                  background: isSelected ? '#18181b' : '#0a0a0a',
                  border: isSelected ? '1px solid #ffffff' : '1px solid var(--border-subtle)',
                  borderRadius: '8px',
                  padding: '12px 14px',
                  cursor: 'pointer',
                  transition: 'all 0.15s'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                  <span className="font-mono" style={{ fontSize: '0.75rem', color: '#ffffff', fontWeight: 700 }}>
                    {c.paymentId}
                  </span>
                  <span className={`badge ${c.severity === 'CRITICAL' ? 'badge-critical' : 'badge-high'}`}>
                    {c.severity}
                  </span>
                </div>

                <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#ffffff', marginBottom: '4px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {c.title}
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '6px' }}>
                  <div className="font-display" style={{ fontSize: '0.95rem', fontWeight: 800, color: isGhostDebit ? '#ef4444' : '#ffffff' }}>
                    ₹{Number(c.amount || 0).toLocaleString('en-IN')}
                  </div>
                  <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>
                    {c.merchantName || c.merchantId}
                  </span>
                </div>
              </div>
            );
          })
        ) : (
          <EmptyState
            title="No Incidents"
            description="No active incidents match your filter."
            icon="search"
            actionLabel="Clear Filter"
            onAction={() => { setSearch(''); setSeverityFilter('ALL'); }}
          />
        )}
      </div>
    </div>
  );
}
