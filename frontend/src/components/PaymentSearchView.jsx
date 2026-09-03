import React, { useState } from 'react';
import {
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  Clock,
  ArrowRight,
  ShieldAlert,
  Download,
  RotateCcw
} from 'lucide-react';

export default function PaymentSearchView({
  cases,
  onSelectCase
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');
  const [selectedMethod, setSelectedMethod] = useState('ALL');

  const filteredCases = cases.filter((c) => {
    const matchesSearch =
      searchTerm === '' ||
      c.paymentId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.orderId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (c.bank?.utr && c.bank.utr.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (c.customerName && c.customerName.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (c.merchantName && c.merchantName.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesSeverity = selectedSeverity === 'ALL' || c.severity === selectedSeverity;
    const matchesMethod = selectedMethod === 'ALL' || c.paymentMethod === selectedMethod;

    return matchesSearch && matchesSeverity && matchesMethod;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', padding: '28px', overflowY: 'auto', height: '100%' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
            <span className="badge badge-outline">GLOBAL SEARCH</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>MULTI-PARTY PAYMENT TELEMETRY SEARCH</span>
          </div>
          <h1 className="font-display" style={{ fontSize: '1.6rem', fontWeight: 800, color: '#ffffff' }}>
            Payment Explorer & Discrepancy Finder
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '2px' }}>
            Search across UTRs, payment IDs, order references, customer accounts, and bank switches.
          </p>
        </div>
      </div>

      {/* Filter Bar */}
      <div style={{
        background: '#0a0a0a',
        border: '1px solid var(--border-subtle)',
        borderRadius: '10px',
        padding: '16px',
        display: 'flex',
        flexWrap: 'wrap',
        gap: '14px',
        alignItems: 'center'
      }}>
        {/* Search Input */}
        <div style={{
          flex: '1 1 300px',
          background: '#121212',
          border: '1px solid var(--border-subtle)',
          borderRadius: '8px',
          padding: '8px 14px',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}>
          <Search size={16} style={{ color: 'var(--text-dim)' }} />
          <input
            type="text"
            placeholder="Search UTR, Payment ID, Order ID, Customer, or Merchant..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{
              background: 'none',
              border: 'none',
              color: '#ffffff',
              fontSize: '0.85rem',
              width: '100%',
              outline: 'none'
            }}
          />
        </div>

        {/* Severity Filter */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Severity:</span>
          <select
            value={selectedSeverity}
            onChange={(e) => setSelectedSeverity(e.target.value)}
            style={{
              background: '#121212',
              border: '1px solid var(--border-subtle)',
              color: '#ffffff',
              padding: '8px 12px',
              borderRadius: '6px',
              fontSize: '0.82rem',
              outline: 'none'
            }}
          >
            <option value="ALL">All Severities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
          </select>
        </div>

        {/* Method Filter */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-dim)', textTransform: 'uppercase' }}>Method:</span>
          <select
            value={selectedMethod}
            onChange={(e) => setSelectedMethod(e.target.value)}
            style={{
              background: '#121212',
              border: '1px solid var(--border-subtle)',
              color: '#ffffff',
              padding: '8px 12px',
              borderRadius: '6px',
              fontSize: '0.82rem',
              outline: 'none'
            }}
          >
            <option value="ALL">All Methods</option>
            <option value="UPI">UPI</option>
            <option value="CREDIT_CARD">Credit Card</option>
            <option value="NETBANKING">Netbanking</option>
            <option value="WALLET">Wallet</option>
          </select>
        </div>

        {/* Reset */}
        {(searchTerm || selectedSeverity !== 'ALL' || selectedMethod !== 'ALL') && (
          <button
            onClick={() => { setSearchTerm(''); setSelectedSeverity('ALL'); setSelectedMethod('ALL'); }}
            className="btn btn-outline-white btn-sm"
          >
            <RotateCcw size={13} /> Reset
          </button>
        )}
      </div>

      {/* Results Table */}
      <div style={{
        background: '#0a0a0a',
        border: '1px solid var(--border-subtle)',
        borderRadius: '12px',
        overflow: 'hidden'
      }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.85rem' }}>
          <thead>
            <tr style={{ background: '#050505', borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-dim)', fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              <th style={{ padding: '14px 18px' }}>Payment / Order</th>
              <th style={{ padding: '14px 18px' }}>Customer & Merchant</th>
              <th style={{ padding: '14px 18px' }}>Amount (INR)</th>
              <th style={{ padding: '14px 18px' }}>Bank Switch</th>
              <th style={{ padding: '14px 18px' }}>Gateway PSP</th>
              <th style={{ padding: '14px 18px' }}>Merchant OMS</th>
              <th style={{ padding: '14px 18px' }}>Severity</th>
              <th style={{ padding: '14px 18px', textAlign: 'right' }}>Action</th>
            </tr>
          </thead>
          <tbody>
            {filteredCases.map((c) => {
              const isGhostDebit = c.incidentType === 'BANK_DEBIT_GATEWAY_FAILURE';
              return (
                <tr
                  key={c.incidentId}
                  style={{ borderBottom: '1px solid var(--border-subtle)', transition: 'background 0.2s' }}
                  onMouseEnter={(e) => { e.currentTarget.style.background = '#121212'; }}
                  onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
                >
                  <td style={{ padding: '14px 18px' }}>
                    <div className="font-mono" style={{ fontWeight: 700, color: '#ffffff' }}>{c.paymentId}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>{c.orderId}</div>
                  </td>
                  <td style={{ padding: '14px 18px' }}>
                    <div style={{ color: '#ffffff', fontWeight: 600 }}>{c.customerName || 'Rahul Sharma'}</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{c.merchantName || c.merchantId}</div>
                  </td>
                  <td style={{ padding: '14px 18px' }}>
                    <div className="font-display" style={{ fontWeight: 800, color: '#ffffff' }}>
                      ₹{Number(c.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>{c.paymentMethod}</div>
                  </td>
                  <td style={{ padding: '14px 18px' }}>
                    <span className="badge badge-success font-mono" style={{ fontSize: '0.7rem' }}>
                      {c.bank?.status || 'SUCCESS'}
                    </span>
                    <div className="font-mono" style={{ fontSize: '0.7rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                      {c.bank?.utr || '414960264709'}
                    </div>
                  </td>
                  <td style={{ padding: '14px 18px' }}>
                    <span className={`badge font-mono ${isGhostDebit ? 'badge-critical' : 'badge-success'}`} style={{ fontSize: '0.7rem' }}>
                      {c.gateway?.status || 'PENDING'}
                    </span>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                      {c.gateway?.gatewayName || 'Razorpay'}
                    </div>
                  </td>
                  <td style={{ padding: '14px 18px' }}>
                    <span className={`badge font-mono ${c.merchant?.status === 'CANCELLED' ? 'badge-critical' : 'badge-outline'}`} style={{ fontSize: '0.7rem' }}>
                      {c.merchant?.status || 'CANCELLED'}
                    </span>
                  </td>
                  <td style={{ padding: '14px 18px' }}>
                    <span className={`badge ${c.severity === 'CRITICAL' ? 'badge-critical' : 'badge-high'}`}>
                      {c.severity}
                    </span>
                  </td>
                  <td style={{ padding: '14px 18px', textAlign: 'right' }}>
                    <button
                      onClick={() => onSelectCase(c.incidentId)}
                      className="btn btn-white btn-sm"
                      style={{ padding: '5px 12px' }}
                    >
                      Investigate <ArrowRight size={12} />
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>

        {filteredCases.length === 0 && (
          <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
            No payment incidents matched your search filter.
          </div>
        )}
      </div>
    </div>
  );
}
