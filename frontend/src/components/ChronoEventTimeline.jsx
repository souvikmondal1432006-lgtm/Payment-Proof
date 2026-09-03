import React, { useState } from 'react';
import {
  Clock,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Send,
  Building2,
  Server,
  Store,
  ArrowRight,
  Info,
  ChevronRight
} from 'lucide-react';

export default function ChronoEventTimeline({
  incident,
  onSelectEvent,
  selectedEventId
}) {
  if (!incident) return null;

  const events = [
    {
      id: 'evt_001',
      time: '14:32:18.120',
      source: 'CUSTOMER / BROWSER',
      sourceId: 'CUSTOMER',
      event: 'PAYMENT STARTED',
      detail: `Checkout initiated for ₹${Number(incident.amount || 0).toLocaleString('en-IN')} via ${incident.paymentMethod || 'UPI'}.`,
      status: 'SUCCESS',
      icon: Clock,
      color: '#ffffff'
    },
    {
      id: 'evt_002',
      time: '14:32:19.540',
      source: 'BANK',
      sourceId: 'BANK',
      event: `₹${Number(incident.amount || 0).toLocaleString('en-IN')} DEBITED`,
      detail: `Core banking switch approved debit under UTR ${incident.bank?.utr || '414960264709'}.`,
      status: 'SUCCESS',
      icon: Building2,
      color: '#10b981'
    },
    {
      id: 'evt_003',
      time: '14:32:20.100',
      source: 'GATEWAY',
      sourceId: 'GATEWAY',
      event: 'PAYMENT PENDING (TIMEOUT)',
      detail: 'Gateway awaiting asynchronous PSP confirmation. Latency counter reached 65,000ms.',
      status: 'WARNING',
      icon: Server,
      color: '#f59e0b'
    },
    {
      id: 'evt_004',
      time: '14:32:21.050',
      source: 'MERCHANT',
      sourceId: 'MERCHANT',
      event: 'ORDER CANCELLED',
      detail: 'Merchant checkout session expired. Inventory seat allocation released.',
      status: 'CRITICAL',
      icon: Store,
      color: '#ef4444'
    },
    {
      id: 'evt_005',
      time: '14:32:42.890',
      source: 'BANK',
      sourceId: 'BANK',
      event: 'BANK PAYMENT SUCCESS ACK',
      detail: 'Delayed ISO-8583 0210 response confirmed clearing completion.',
      status: 'SUCCESS',
      icon: CheckCircle2,
      color: '#10b981'
    },
    {
      id: 'evt_006',
      time: '14:32:43.200',
      source: 'GATEWAY / WEBHOOK',
      sourceId: 'WEBHOOK',
      event: 'WEBHOOK NOT RECEIVED (504 DROPPED)',
      detail: 'Asynchronous webhook delivery failed with HTTP 504 after 3 attempts.',
      status: 'CRITICAL',
      icon: Send,
      color: '#ef4444'
    }
  ];

  return (
    <div style={{
      background: '#09090b',
      border: '1px solid var(--border-subtle)',
      borderRadius: '14px',
      padding: '20px'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <h3 className="font-display" style={{ fontSize: '1rem', fontWeight: 800, color: '#ffffff' }}>
              Forensic Chronological Timeline
            </h3>
            <span className="badge badge-outline" style={{ fontSize: '0.65rem' }}>
              Millisecond Precision
            </span>
          </div>
          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
            Exact cross-system event sequence showing the exact moment the transaction diverged
          </div>
        </div>
      </div>

      {/* Timeline Stream */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', position: 'relative' }}>
        {events.map((evt, idx) => {
          const Icon = evt.icon;
          const isSelected = selectedEventId === evt.id;

          return (
            <div
              key={evt.id}
              onClick={() => onSelectEvent && onSelectEvent(evt)}
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '14px',
                padding: '12px 14px',
                background: isSelected ? 'rgba(255, 255, 255, 0.08)' : '#040404',
                border: `1px solid ${isSelected ? '#ffffff' : 'var(--border-subtle)'}`,
                borderRadius: '8px',
                cursor: 'pointer',
                transition: 'all 0.15s'
              }}
              className="surface-interactive"
            >
              {/* Timestamp */}
              <div className="font-mono" style={{
                fontSize: '0.75rem',
                fontWeight: 700,
                color: 'var(--text-dim)',
                width: '95px',
                flexShrink: 0,
                paddingTop: '2px'
              }}>
                {evt.time}
              </div>

              {/* Source Icon Badge */}
              <div style={{
                width: '26px',
                height: '26px',
                borderRadius: '6px',
                background: 'rgba(255, 255, 255, 0.05)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: evt.color,
                flexShrink: 0
              }}>
                <Icon size={14} />
              </div>

              {/* Event Content */}
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2px' }}>
                  <span className="badge" style={{
                    fontSize: '0.65rem',
                    background: 'rgba(255,255,255,0.06)',
                    color: evt.color,
                    padding: '2px 6px'
                  }}>
                    {evt.source}
                  </span>
                  <div className="font-display" style={{ fontSize: '0.85rem', fontWeight: 700, color: '#ffffff' }}>
                    {evt.event}
                  </div>
                </div>

                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', lineHeight: 1.4 }}>
                  {evt.detail}
                </div>
              </div>

              <ChevronRight size={15} style={{ color: 'var(--text-dim)', alignSelf: 'center' }} />
            </div>
          );
        })}
      </div>
    </div>
  );
}
