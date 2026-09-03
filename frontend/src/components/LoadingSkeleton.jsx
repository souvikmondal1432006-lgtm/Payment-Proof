import React from 'react';

export default function LoadingSkeleton({ type = 'card', count = 1 }) {
  const items = Array.from({ length: count });

  if (type === 'row') {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {items.map((_, i) => (
          <div
            key={i}
            style={{
              height: '52px',
              background: 'rgba(255, 255, 255, 0.03)',
              borderRadius: '6px',
              border: '1px solid var(--border-subtle)',
              animation: 'pulseGlow 1.5s infinite ease-in-out'
            }}
          />
        ))}
      </div>
    );
  }

  if (type === 'stream') {
    return (
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px' }}>
        {[1, 2, 3, 4].map((n) => (
          <div
            key={n}
            style={{
              height: '90px',
              background: 'rgba(255, 255, 255, 0.03)',
              borderRadius: '8px',
              border: '1px solid var(--border-subtle)',
              animation: 'pulseGlow 1.5s infinite ease-in-out'
            }}
          />
        ))}
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
      {items.map((_, i) => (
        <div
          key={i}
          style={{
            height: '140px',
            background: 'rgba(255, 255, 255, 0.03)',
            borderRadius: '12px',
            border: '1px solid var(--border-subtle)',
            animation: 'pulseGlow 1.5s infinite ease-in-out'
          }}
        />
      ))}
    </div>
  );
}
