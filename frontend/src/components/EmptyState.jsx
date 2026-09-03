import React from 'react';
import { Search, Inbox, ShieldCheck, RefreshCw } from 'lucide-react';

export default function EmptyState({
  title = "No Records Found",
  description = "No payment or incident records match the current criteria.",
  icon = "inbox",
  actionLabel,
  onAction
}) {
  const renderIcon = () => {
    switch (icon) {
      case 'search': return <Search size={36} style={{ color: 'var(--text-dim)' }} />;
      case 'shield': return <ShieldCheck size={36} style={{ color: '#10b981' }} />;
      default: return <Inbox size={36} style={{ color: 'var(--text-dim)' }} />;
    }
  };

  return (
    <div style={{
      background: '#0a0a0a',
      border: '1px dashed var(--border-subtle)',
      borderRadius: '12px',
      padding: '48px 24px',
      textAlign: 'center',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: '12px'
    }}>
      <div style={{
        width: '56px',
        height: '56px',
        borderRadius: '50%',
        background: 'rgba(255, 255, 255, 0.03)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
      }}>
        {renderIcon()}
      </div>

      <div>
        <div className="font-display" style={{ fontSize: '1.1rem', fontWeight: 700, color: '#ffffff', marginBottom: '4px' }}>
          {title}
        </div>
        <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', maxWidth: '420px', lineHeight: 1.5 }}>
          {description}
        </div>
      </div>

      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="btn btn-outline-white btn-sm"
          style={{ marginTop: '8px' }}
        >
          <RefreshCw size={13} />
          {actionLabel}
        </button>
      )}
    </div>
  );
}
