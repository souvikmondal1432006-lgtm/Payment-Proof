import React from 'react';
import {
  ShieldCheck,
  Activity,
  Database,
  Cpu,
  RefreshCw,
  PlayCircle,
  FileText,
  Search,
  Layers,
  Clock,
  Zap,
  Sparkles,
  Server,
  LogOut
} from 'lucide-react';

export default function Header({
  systemHealth,
  currentUser,
  onRefresh,
  onOpenSimulator,
  activeView,
  setActiveView,
  onLogout,
  onBackToLanding
}) {
  const isMlUp = systemHealth?.dependencies?.mlService?.status === 'UP';
  const isDbUp = systemHealth?.dependencies?.database === 'UP';

  const navItems = [
    { id: 'command_center', label: 'Command Center', icon: <Activity size={14} /> },
    { id: 'investigation', label: 'Investigation', icon: <Layers size={14} /> },
    { id: 'search', label: 'Search', icon: <Search size={14} /> },
    { id: 'timeline', label: 'Timeline', icon: <Clock size={14} /> },
    { id: 'evidence', label: 'Evidence', icon: <Database size={14} /> },
    { id: 'ai_conclusion', label: 'AI Report', icon: <Sparkles size={14} /> },
    { id: 'resolution', label: 'Resolution', icon: <ShieldCheck size={14} /> },
    { id: 'system_health', label: 'Health', icon: <Server size={14} /> },
    { id: 'audit', label: 'Audit Log', icon: <FileText size={14} /> }
  ];

  return (
    <header style={{
      height: '68px',
      background: '#000000',
      borderBottom: '1px solid var(--border-subtle)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 24px',
      zIndex: 40
    }}>
      {/* Brand & Nav */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
        <div
          onClick={onBackToLanding}
          style={{ display: 'flex', alignItems: 'center', gap: '10px', cursor: 'pointer' }}
          title="Back to Landing Page"
        >
          <div style={{
            width: '32px',
            height: '32px',
            background: '#ffffff',
            borderRadius: '6px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#000000'
          }}>
            <ShieldCheck size={20} strokeWidth={2.5} />
          </div>
          <div>
            <div className="font-display" style={{ color: '#ffffff', fontWeight: 800, fontSize: '1rem', letterSpacing: '-0.02em' }}>
              PAYMENT PROOF
            </div>
            <div style={{ fontSize: '0.65rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Forensic Workstation
            </div>
          </div>
        </div>

        {/* Global Navigation Switcher */}
        <div style={{ display: 'flex', background: '#0a0a0a', padding: '3px', borderRadius: '8px', border: '1px solid var(--border-subtle)', overflowX: 'auto' }}>
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => setActiveView(item.id)}
              className={`btn btn-sm ${activeView === item.id ? 'btn-white' : 'btn-outline-white'}`}
              style={{
                border: 'none',
                padding: '5px 12px',
                fontSize: '0.78rem',
                whiteSpace: 'nowrap',
                gap: '6px'
              }}
            >
              {item.icon}
              {item.label}
            </button>
          ))}
        </div>
      </div>

      {/* Right Controls */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        {/* Anomaly Simulator */}
        <button
          onClick={onOpenSimulator}
          className="btn btn-outline-white btn-sm"
          style={{ borderColor: 'rgba(255,255,255,0.25)' }}
        >
          <PlayCircle size={14} style={{ color: '#f59e0b' }} />
          Simulate Anomaly
        </button>

        {/* Refresh */}
        <button
          onClick={onRefresh}
          className="btn btn-outline-white btn-sm"
          title="Refresh Data"
        >
          <RefreshCw size={13} />
        </button>

        {/* Operator Profile & Sign-out */}
        {currentUser && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', paddingLeft: '6px', borderLeft: '1px solid var(--border-subtle)' }}>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#ffffff' }}>
                {currentUser.name}
              </div>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)' }}>
                {currentUser.clearance}
              </div>
            </div>
            <button
              onClick={onLogout}
              className="btn btn-outline-white btn-sm"
              title="Sign Out"
              style={{ padding: '6px 8px' }}
            >
              <LogOut size={14} />
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
