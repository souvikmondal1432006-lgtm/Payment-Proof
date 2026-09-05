import React, { useState, useEffect } from 'react';
import {
  Clock,
  Shield,
  Server,
  ShoppingCart,
  Send,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Activity,
  Layers,
  ArrowRight,
  HelpCircle
} from 'lucide-react';
import { api } from '../services/api';

export default function TimelineView({ incident }) {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (incident?.paymentId) {
      loadTimeline(incident.paymentId);
    }
  }, [incident]);

  const loadTimeline = async (paymentId) => {
    setLoading(true);
    try {
      const data = await api.getPaymentTimeline(paymentId);
      setEvents(data || []);
    } catch (e) {
      console.error('Failed to load payment timeline:', e);
    } finally {
      setLoading(false);
    }
  };

  if (!incident) return null;

  const formatEventTime = (timestamp) => {
    if (!timestamp) return '—';
    try {
      const d = new Date(timestamp);
      if (isNaN(d.getTime())) return '—';
      const timeStr = d.toLocaleTimeString('en-IN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
      const ms = String(d.getMilliseconds()).padStart(3, '0');
      return `${timeStr}.${ms}`;
    } catch {
      return '—';
    }
  };

  const getSourceIcon = (source) => {
    const s = String(source || '').toUpperCase();
    if (s.includes('BANK')) return <Shield size={16} />;
    if (s.includes('GATEWAY')) return <Server size={16} />;
    if (s.includes('MERCHANT') || s.includes('CART') || s.includes('OMS')) return <ShoppingCart size={16} />;
    if (s.includes('WEBHOOK')) return <Send size={16} />;
    return <Activity size={16} />;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', padding: '28px', overflowY: 'auto', height: '100%' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
            <span className="badge badge-outline font-mono">{incident.paymentId}</span>
            <span className="badge badge-white">TIMELINE CHRONOLOGY</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>MILLISECOND FORENSIC TRACE</span>
          </div>
          <h1 className="font-display" style={{ fontSize: '1.6rem', fontWeight: 800, color: '#ffffff' }}>
            Chronological Payment Event Sequence
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginTop: '2px' }}>
            Event trace reconstructed from Customer App, Core Banking Switch, Gateway PSP, and Merchant OMS.
          </p>
        </div>

        <span className="badge badge-outline font-mono">
          {events.length} Forensic Events
        </span>
      </div>

      {/* Forensic Story Trajectory Bar */}
      <div style={{
        background: '#0d0d0d',
        border: '1px solid var(--border-subtle)',
        borderRadius: '10px',
        padding: '16px 20px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: '10px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#38bdf8' }} />
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#ffffff' }}>CUSTOMER PAYMENT</span>
        </div>
        <ArrowRight size={14} style={{ color: 'var(--text-dim)' }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10b981' }} />
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#10b981' }}>BANK SUCCESS</span>
        </div>
        <ArrowRight size={14} style={{ color: 'var(--text-dim)' }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10b981' }} />
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#10b981' }}>₹{incident.amount?.toLocaleString('en-IN')} DEBITED</span>
        </div>
        <ArrowRight size={14} style={{ color: 'var(--text-dim)' }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#ef4444' }} />
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#ef4444' }}>GATEWAY TIMEOUT</span>
        </div>
        <ArrowRight size={14} style={{ color: 'var(--text-dim)' }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#f59e0b' }} />
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#f59e0b' }}>MERCHANT CANCELLED</span>
        </div>
        <ArrowRight size={14} style={{ color: 'var(--text-dim)' }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#ef4444' }} />
          <span style={{ fontSize: '0.75rem', fontWeight: 700, color: '#ef4444' }}>WEBHOOK DROPPED</span>
        </div>
      </div>

      {/* Interactive Timeline Stream */}
      <div style={{
        background: '#0a0a0a',
        border: '1px solid var(--border-subtle)',
        borderRadius: '12px',
        padding: '24px',
        display: 'flex',
        flexDirection: 'column',
        gap: '16px'
      }}>
        {events.map((evt, idx) => {
          const rawPrev = evt.previousState || evt.fromStatus;
          const prevState = (rawPrev && String(rawPrev).trim() !== '' && String(rawPrev).toUpperCase() !== 'NONE')
            ? rawPrev
            : (idx === 0 ? 'Initial state' : '—');
          const nextState = evt.newState || evt.toStatus || evt.status || 'RECORDED';

          const stateUpper = String(nextState).toUpperCase();
          const isError = stateUpper.includes('TIMEOUT') || stateUpper.includes('FAILED') || stateUpper.includes('CANCELLED') || stateUpper.includes('DROPPED');
          const isSuccess = stateUpper.includes('DEBITED') || stateUpper.includes('CAPTURED') || stateUpper.includes('SUCCESS') || stateUpper.includes('PAID');

          const sourceLabel = evt.sourceSystem || evt.source || 'SYSTEM';
          const eventTime = evt.timestamp || evt.eventTimestamp || evt.eventTime;

          return (
            <div
              key={evt.eventId || idx}
              style={{
                display: 'flex',
                gap: '16px',
                position: 'relative'
              }}
            >
              {/* Left Timeline Indicator */}
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <div style={{
                  width: '34px',
                  height: '34px',
                  borderRadius: '50%',
                  background: isError ? 'rgba(239, 68, 68, 0.2)' : isSuccess ? 'rgba(16, 185, 129, 0.2)' : 'rgba(255, 255, 255, 0.08)',
                  border: isError ? '1px solid #ef4444' : isSuccess ? '1px solid #10b981' : '1px solid var(--border-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: isError ? '#ef4444' : isSuccess ? '#10b981' : '#ffffff',
                  fontSize: '0.8rem',
                  fontWeight: 700
                }}>
                  {getSourceIcon(sourceLabel)}
                </div>
                {idx < events.length - 1 && (
                  <div style={{ width: '2px', flex: 1, background: 'var(--border-subtle)', margin: '4px 0', minHeight: '24px' }} />
                )}
              </div>

              {/* Event Content Card */}
              <div style={{
                flex: 1,
                background: '#121212',
                border: isError ? '1px solid rgba(239, 68, 68, 0.35)' : isSuccess ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid var(--border-subtle)',
                borderRadius: '8px',
                padding: '16px 20px',
                marginBottom: '10px'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px', flexWrap: 'wrap', gap: '8px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '0.95rem', fontWeight: 700, color: '#ffffff' }}>
                      {evt.title || evt.eventType?.replace(/_/g, ' ')}
                    </span>
                    <span className="badge badge-outline font-mono" style={{ fontSize: '0.68rem', color: '#38bdf8', borderColor: 'rgba(56, 189, 248, 0.3)' }}>
                      {sourceLabel}
                    </span>
                    {evt.latencyMs > 0 && (
                      <span className="font-mono" style={{
                        fontSize: '0.72rem',
                        padding: '2px 8px',
                        borderRadius: '4px',
                        background: evt.latencyMs >= 10000 ? 'rgba(239, 68, 68, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                        color: evt.latencyMs >= 10000 ? '#ef4444' : '#f59e0b',
                        border: evt.latencyMs >= 10000 ? '1px solid rgba(239, 68, 68, 0.3)' : '1px solid rgba(245, 158, 11, 0.3)'
                      }}>
                        Latency: {evt.latencyMs >= 1000 ? `${(evt.latencyMs / 1000).toFixed(1)}s` : `${evt.latencyMs}ms`}
                      </span>
                    )}
                  </div>

                  <span className="font-mono" style={{ fontSize: '0.78rem', color: '#94a3b8', background: 'rgba(255, 255, 255, 0.04)', padding: '3px 8px', borderRadius: '4px' }}>
                    {formatEventTime(eventTime)}
                  </span>
                </div>

                {/* State Transition Row */}
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  fontSize: '0.82rem',
                  color: 'var(--text-muted)',
                  background: '#090909',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  border: '1px solid rgba(255, 255, 255, 0.05)',
                  marginBottom: '8px',
                  flexWrap: 'wrap'
                }}>
                  <span>State: <strong style={{ color: '#94a3b8' }}>{prevState}</strong></span>
                  <ArrowRight size={13} style={{ color: 'var(--text-muted)' }} />
                  <span>New State: <strong style={{ color: isError ? '#ef4444' : isSuccess ? '#10b981' : '#ffffff' }}>{nextState}</strong></span>
                </div>

                {/* Detailed Forensic Description */}
                {evt.description && (
                  <div style={{ fontSize: '0.84rem', color: 'var(--text-dim)', lineHeight: 1.5, marginTop: '6px' }}>
                    {evt.description}
                  </div>
                )}

                {/* Metadata JSON or key-values */}
                {evt.metadata && Object.keys(evt.metadata).length > 0 && (
                  <div className="font-mono" style={{
                    marginTop: '10px',
                    padding: '8px 12px',
                    background: '#050505',
                    borderRadius: '4px',
                    fontSize: '0.72rem',
                    color: 'var(--text-dim)',
                    border: '1px solid rgba(255, 255, 255, 0.04)',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap'
                  }}>
                    {typeof evt.metadata === 'object' ? JSON.stringify(evt.metadata) : evt.metadata}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
