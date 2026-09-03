import React, { useState } from 'react';
import { AlertTriangle, RefreshCw, ChevronRight, Terminal, X, ShieldAlert } from 'lucide-react';

export default function ErrorBanner({
  error,
  onRetry,
  onDismiss,
  title = "Investigation Notice"
}) {
  const [showDiagnostics, setShowDiagnostics] = useState(false);

  if (!error) return null;

  // Extract human message and diagnostic details
  const humanMessage = error.humanMessage || error.message || "We couldn't complete this action right now. Your payment records remain safe. Try again.";
  const technicalDetails = error.technicalDetails || error.stack || (typeof error === 'object' ? JSON.stringify(error, null, 2) : String(error));
  const statusCode = error.status || error.statusCode || (error.response ? error.response.status : null);
  const endpoint = error.endpoint || error.url || null;

  return (
    <div style={{
      background: 'rgba(239, 68, 68, 0.08)',
      border: '1px solid rgba(239, 68, 68, 0.3)',
      borderRadius: '10px',
      padding: '16px 20px',
      marginBottom: '16px',
      display: 'flex',
      flexDirection: 'column',
      gap: '10px'
    }}>
      {/* Top Main Row */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '14px' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
          <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '6px',
            background: 'rgba(239, 68, 68, 0.2)',
            color: '#ef4444',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0
          }}>
            <ShieldAlert size={18} />
          </div>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2px' }}>
              <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#ffffff' }}>
                {title}
              </span>
              {statusCode && (
                <span className="badge badge-critical font-mono" style={{ fontSize: '0.68rem' }}>
                  HTTP {statusCode}
                </span>
              )}
            </div>

            <div style={{ fontSize: '0.88rem', color: '#fca5a5', lineHeight: 1.5 }}>
              {humanMessage}
            </div>
          </div>
        </div>

        {/* Action Controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {onRetry && (
            <button
              onClick={onRetry}
              className="btn btn-outline-white btn-sm"
              style={{ padding: '6px 12px', fontSize: '0.78rem' }}
            >
              <RefreshCw size={13} />
              Retry
            </button>
          )}

          {onDismiss && (
            <button
              onClick={onDismiss}
              style={{ background: 'none', border: 'none', color: 'var(--text-dim)', cursor: 'pointer', padding: '4px' }}
              title="Dismiss"
            >
              <X size={16} />
            </button>
          )}
        </div>
      </div>

      {/* Expandable Technical Diagnostics Accordion */}
      <div style={{ borderTop: '1px solid rgba(239, 68, 68, 0.15)', paddingTop: '8px', marginTop: '4px' }}>
        <button
          onClick={() => setShowDiagnostics(!showDiagnostics)}
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--text-dim)',
            fontSize: '0.75rem',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            padding: 0
          }}
        >
          <ChevronRight size={13} style={{ transform: showDiagnostics ? 'rotate(90deg)' : 'none', transition: 'transform 0.2s' }} />
          <span>{showDiagnostics ? 'Hide Technical Diagnostics' : 'View Diagnostic Details (Status, Headers, Payload)'}</span>
        </button>

        {showDiagnostics && (
          <div style={{ marginTop: '8px' }}>
            {endpoint && (
              <div className="font-mono" style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginBottom: '4px' }}>
                Endpoint: <span style={{ color: '#ffffff' }}>{endpoint}</span>
              </div>
            )}
            <pre className="font-mono" style={{
              background: '#050505',
              border: '1px solid var(--border-subtle)',
              borderRadius: '6px',
              padding: '10px',
              fontSize: '0.72rem',
              color: '#a1a1aa',
              overflowX: 'auto',
              maxHeight: '160px',
              whiteSpace: 'pre-wrap'
            }}>
              {typeof technicalDetails === 'string' ? technicalDetails : JSON.stringify(technicalDetails, null, 2)}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
}
