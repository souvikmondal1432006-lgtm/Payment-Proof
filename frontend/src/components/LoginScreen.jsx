import React, { useState } from 'react';
import { ShieldCheck, Lock, User, Key, ArrowRight, ArrowLeft, CheckCircle2, Fingerprint, Sparkles } from 'lucide-react';

export default function LoginScreen({ onLoginSuccess, onBackToLanding }) {
  const [operatorId, setOperatorId] = useState('operator_priya_m');
  const [password, setPassword] = useState('••••••••••••');
  const [loading, setLoading] = useState(false);

  const presetProfiles = [
    {
      id: 'operator_priya_m',
      name: 'Priya Mukherjee',
      role: 'Lead Forensic Investigator',
      clearance: 'Tier-3 Authoritative',
      avatar: 'PM'
    },
    {
      id: 'operator_arjun_k',
      name: 'Arjun Kapoor',
      role: 'FinTech Settlement Specialist',
      clearance: 'Tier-2 Settlement',
      avatar: 'AK'
    },
    {
      id: 'sre_compliance_lead',
      name: 'Kavita Sharma',
      role: 'SRE & Audit Controller',
      clearance: 'Tier-1 Root Audit',
      avatar: 'KS'
    }
  ];

  const handleLogin = (e) => {
    e.preventDefault();
    setLoading(true);
    setTimeout(() => {
      const selected = presetProfiles.find(p => p.id === operatorId) || {
        id: operatorId,
        name: operatorId,
        role: 'Authorized Operator',
        clearance: 'Tier-2 Standard',
        avatar: 'OP'
      };
      onLoginSuccess(selected);
      setLoading(false);
    }, 400);
  };

  const handleQuickSelect = (profile) => {
    setOperatorId(profile.id);
    setLoading(true);
    setTimeout(() => {
      onLoginSuccess(profile);
      setLoading(false);
    }, 300);
  };

  return (
    <div style={{
      minHeight: '100vh',
      background: '#000000',
      color: '#ffffff',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center',
      padding: '24px',
      position: 'relative'
    }}>
      {/* Background Subtle Luminescence */}
      <div style={{
        position: 'absolute',
        top: '20%',
        width: '400px',
        height: '400px',
        background: 'radial-gradient(circle, rgba(255,255,255,0.06) 0%, transparent 70%)',
        pointerEvents: 'none'
      }} />

      {/* Back button */}
      <button
        onClick={onBackToLanding}
        className="btn btn-outline-white btn-sm"
        style={{ position: 'absolute', top: '24px', left: '24px' }}
      >
        <ArrowLeft size={14} /> Back to Overview
      </button>

      <div style={{ width: '100%', maxWidth: '440px', position: 'relative', zIndex: 1 }}>
        {/* Logo & Header */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div style={{
            width: '48px',
            height: '48px',
            background: '#ffffff',
            borderRadius: '12px',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#000000',
            marginBottom: '16px'
          }}>
            <ShieldCheck size={28} strokeWidth={2.5} />
          </div>
          <h2 className="font-display" style={{ fontSize: '1.6rem', fontWeight: 800, letterSpacing: '-0.03em' }}>
            OPERATOR CLEARANCE
          </h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            Authoritative Payment Inconsistency Investigation Console
          </p>
        </div>

        {/* Login Card */}
        <div className="surface-card" style={{ padding: '28px', background: '#0a0a0a', border: '1px solid var(--border-subtle)' }}>
          {/* Quick Profile Selector */}
          <div style={{ marginBottom: '24px' }}>
            <label style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '10px', display: 'block' }}>
              Select Active Operator Profile
            </label>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {presetProfiles.map(p => {
                const isSelected = operatorId === p.id;
                return (
                  <div
                    key={p.id}
                    onClick={() => handleQuickSelect(p)}
                    style={{
                      padding: '10px 14px',
                      background: isSelected ? 'rgba(255,255,255,0.08)' : '#050505',
                      border: `1px solid ${isSelected ? '#ffffff' : 'var(--border-subtle)'}`,
                      borderRadius: '8px',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      transition: 'all 0.15s ease'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      <div style={{
                        width: '30px',
                        height: '30px',
                        borderRadius: '6px',
                        background: isSelected ? '#ffffff' : 'rgba(255,255,255,0.1)',
                        color: isSelected ? '#000000' : '#ffffff',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '0.75rem',
                        fontWeight: 800
                      }}>
                        {p.avatar}
                      </div>
                      <div>
                        <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#ffffff' }}>
                          {p.name}
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                          {p.role}
                        </div>
                      </div>
                    </div>

                    <span className="badge badge-outline" style={{ fontSize: '0.65rem' }}>
                      {p.clearance}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', margin: '20px 0' }}>
            <div style={{ height: '1px', flex: 1, background: 'var(--border-subtle)' }} />
            <span style={{ fontSize: '0.7rem', color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>or enter credentials</span>
            <div style={{ height: '1px', flex: 1, background: 'var(--border-subtle)' }} />
          </div>

          {/* Form */}
          <form onSubmit={handleLogin}>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '6px', display: 'block' }}>
                Operator ID
              </label>
              <div style={{ position: 'relative' }}>
                <User size={15} style={{ position: 'absolute', left: '12px', top: '12px', color: 'var(--text-dim)' }} />
                <input
                  type="text"
                  value={operatorId}
                  onChange={(e) => setOperatorId(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 12px 10px 36px',
                    background: '#000000',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: '8px',
                    color: '#ffffff',
                    fontSize: '0.85rem',
                    fontFamily: 'var(--font-mono)'
                  }}
                />
              </div>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <label style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '6px', display: 'block' }}>
                Passkey / Security Token
              </label>
              <div style={{ position: 'relative' }}>
                <Key size={15} style={{ position: 'absolute', left: '12px', top: '12px', color: 'var(--text-dim)' }} />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '10px 12px 10px 36px',
                    background: '#000000',
                    border: '1px solid var(--border-subtle)',
                    borderRadius: '8px',
                    color: '#ffffff',
                    fontSize: '0.85rem',
                    fontFamily: 'var(--font-mono)'
                  }}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-white"
              style={{ width: '100%', padding: '12px', fontWeight: 700 }}
            >
              {loading ? 'Authenticating Clearance...' : 'Authenticate & Enter Console'} <ArrowRight size={15} />
            </button>
          </form>
        </div>

        {/* Security Notice */}
        <div style={{ textAlign: 'center', marginTop: '20px', fontSize: '0.72rem', color: 'var(--text-dim)' }}>
          🔒 Immutable Cryptographic Audit Ledger active on all actions
        </div>
      </div>
    </div>
  );
}
