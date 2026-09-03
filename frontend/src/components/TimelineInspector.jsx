import React from 'react';
import { Clock, Shield, Server, ShoppingCart, Send, AlertTriangle, CheckCircle, XCircle, HelpCircle } from 'lucide-react';

export default function TimelineInspector({ telemetries, incident }) {
  if (!telemetries || telemetries.length === 0) {
    return (
      <div className="surface-card" style={{ padding: '32px', textAlign: 'center', color: 'var(--text-dim)' }}>
        No telemetry records available for this transaction.
      </div>
    );
  }

  const getProviderIcon = (type) => {
    switch (type) {
      case 'BANK': return <Shield size={16} />;
      case 'GATEWAY': return <Server size={16} />;
      case 'MERCHANT_APP': return <ShoppingCart size={16} />;
      case 'WEBHOOK_SERVICE': return <Send size={16} />;
      default: return <Clock size={16} />;
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'SUCCESS':
        return <span className="badge badge-success">SUCCESS</span>;
      case 'FAILED':
      case 'CANCELLED':
      case 'MISSING':
        return <span className="badge badge-critical">{status}</span>;
      case 'PENDING':
        return <span className="badge badge-high">PENDING</span>;
      default:
        return <span className="badge badge-outline">{status}</span>;
    }
  };

  return (
    <div className="surface-card" style={{ padding: '24px', marginBottom: '20px', background: '#090909' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <h3 className="font-display" style={{ fontSize: '1.15rem', fontWeight: 800, color: '#ffffff' }}>
            Multi-Party Telemetry Waterfall
          </h3>
          <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '2px' }}>
            Authoritative state consensus comparison across bank, gateway, merchant, and webhook dispatcher.
          </p>
        </div>

        <span className="badge badge-outline font-mono">
          {telemetries.length} Telemetry Streams
        </span>
      </div>

      {/* Divergence Root Cause Alert Banner */}
      {incident && (
        <div style={{
          background: 'rgba(239, 68, 68, 0.08)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          borderRadius: '8px',
          padding: '12px 16px',
          marginBottom: '20px',
          display: 'flex',
          gap: '12px',
          alignItems: 'flex-start'
        }}>
          <AlertTriangle size={18} style={{ color: '#ef4444', flexShrink: 0, marginTop: '2px' }} />
          <div>
            <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#fca5a5', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              State Divergence Detected • {incident.contradictionType}
            </div>
            <div style={{ fontSize: '0.82rem', color: '#ffffff', marginTop: '3px', lineHeight: 1.4 }}>
              {incident.divergenceSummary}
            </div>
          </div>
        </div>
      )}

      {/* Telemetry Nodes Waterfall */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {telemetries.map((tel, index) => {
          return (
            <div
              key={tel.id || index}
              className="surface-card"
              style={{
                padding: '14px 18px',
                background: '#040404',
                border: '1px solid var(--border-subtle)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: '16px'
              }}
            >
              {/* Left Party Type */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '14px', width: '220px' }}>
                <div style={{
                  width: '34px',
                  height: '34px',
                  borderRadius: '6px',
                  background: 'rgba(255, 255, 255, 0.06)',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  {getProviderIcon(tel.providerType)}
                </div>

                <div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    {tel.providerType?.replace(/_/g, ' ')}
                  </div>
                  <div className="font-display" style={{ fontSize: '0.88rem', fontWeight: 700, color: '#ffffff' }}>
                    {tel.providerName}
                  </div>
                </div>
              </div>

              {/* Status & Code */}
              <div style={{ width: '130px' }}>
                {getStatusBadge(tel.reportedStatus)}
                <div className="font-mono" style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                  Code: <strong style={{ color: '#ffffff' }}>{tel.rawResponseCode || 'N/A'}</strong>
                </div>
              </div>

              {/* Raw Message */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: '0.8rem', color: '#e4e4e7', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {tel.rawResponseMessage || 'No telemetry message recorded'}
                </div>
                {tel.payloadHash && (
                  <div className="font-mono" style={{ fontSize: '0.68rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                    {tel.payloadHash}
                  </div>
                )}
              </div>

              {/* Latency & Timestamp */}
              <div style={{ textAlign: 'right', width: '130px' }}>
                <div className="font-mono" style={{ fontSize: '0.78rem', color: '#ffffff', fontWeight: 600 }}>
                  {tel.latencyMs ? `${tel.latencyMs}ms` : '—'}
                </div>
                <div className="font-mono" style={{ fontSize: '0.68rem', color: 'var(--text-dim)', marginTop: '2px' }}>
                  {tel.eventTimestamp ? new Date(tel.eventTimestamp).toLocaleTimeString() : 'N/A'}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
