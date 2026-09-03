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
  ArrowRight
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
          const isError = evt.newState === 'TIMEOUT' || evt.newState === 'FAILED' || evt.newState === 'CANCELLED';
          const isSuccess = evt.newState === 'DEBITED' || evt.newState === 'CAPTURED' || evt.newState === 'PAID';

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
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  background: isError ? 'rgba(239, 68, 68, 0.2)' : isSuccess ? 'rgba(16, 185, 129, 0.2)' : 'rgba(255, 255, 255, 0.08)',
                  border: isError ? '1px solid #ef4444' : isSuccess ? '1px solid #10b981' : '1px solid var(--border-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: isError ? '#ef4444' : isSuccess ? '#10b981' : '#ffffff',
                  fontSize: '0.75rem',
                  fontWeight: 700
                }}>
                  {idx + 1}
                </div>
                {idx < events.length - 1 && (
                  <div style={{ width: '2px', flex: 1, background: 'var(--border-subtle)', margin: '4px 0' }} />
                )}
              </div>

              {/* Event Content Card */}
              <div style={{
                flex: 1,
                background: '#121212',
                border: isError ? '1px solid rgba(239, 68, 68, 0.3)' : '1px solid var(--border-subtle)',
                borderRadius: '8px',
                padding: '14px 18px',
                marginBottom: '10px'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontSize: '0.9rem', fontWeight: 700, color: '#ffffff' }}>
                      {evt.eventType?.replace(/_/g, ' ')}
                    </span>
                    <span className="badge badge-outline font-mono" style={{ fontSize: '0.68rem' }}>
                      {evt.sourceSystem}
                    </span>
                  </div>

                  <span className="font-mono" style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                    {evt.eventTimestamp ? new Date(evt.eventTimestamp).toLocaleTimeString() : '10:43:00'}
                  </span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  <span>State: <strong style={{ color: '#ffffff' }}>{evt.previousState}</strong></span>
                  <ArrowRight size={12} />
                  <span>New State: <strong style={{ color: isError ? '#ef4444' : isSuccess ? '#10b981' : '#ffffff' }}>{evt.newState}</strong></span>
                  {evt.latencyMs > 0 && (
                    <span className="font-mono" style={{ color: evt.latencyMs > 10000 ? '#ef4444' : '#f59e0b', marginLeft: 'auto' }}>
                      Latency: {evt.latencyMs >= 1000 ? `${(evt.latencyMs / 1000).toFixed(1)}s` : `${evt.latencyMs}ms`}
                    </span>
                  )}
                </div>

                {evt.metadataJson && (
                  <div className="font-mono" style={{
                    marginTop: '8px',
                    padding: '6px 10px',
                    background: '#050505',
                    borderRadius: '4px',
                    fontSize: '0.72rem',
                    color: 'var(--text-dim)',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap'
                  }}>
                    {evt.metadataJson}
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
