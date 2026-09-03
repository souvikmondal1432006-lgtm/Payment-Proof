import React, { useState } from 'react';
import {
  ShieldCheck,
  Search,
  ChevronRight,
  ChevronDown,
  Lock,
  Clock,
  User,
  FileText,
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  Copy,
  Layers,
  Activity
} from 'lucide-react';
import api from '../services/api';

export default function AuditLedgerView({ auditEvents, onSelectEntity }) {
  const [search, setSearch] = useState('');
  const [expandedId, setExpandedId] = useState(null);
  const [verifying, setVerifying] = useState(false);
  const [verificationResult, setVerificationResult] = useState(null);
  const [copiedHash, setCopiedHash] = useState(null);

  const filteredEvents = (auditEvents || []).filter(e => {
    const q = search.toLowerCase();
    return !q ||
      e.auditId?.toLowerCase().includes(q) ||
      e.id?.toLowerCase().includes(q) ||
      e.entityId?.toLowerCase().includes(q) ||
      e.entityName?.toLowerCase().includes(q) ||
      e.action?.toLowerCase().includes(q) ||
      e.actorId?.toLowerCase().includes(q);
  });

  const toggleExpand = (id) => {
    setExpandedId(expandedId === id ? null : id);
  };

  const handleVerifyChain = async () => {
    setVerifying(true);
    try {
      const result = await api.verifyAuditChain();
      setVerificationResult(result);
    } catch (err) {
      setVerificationResult({
        isValid: false,
        verificationSummary: 'Unable to reach verification endpoint. Check backend service.'
      });
    } finally {
      setVerifying(false);
    }
  };

  const copyToClipboard = (text, key) => {
    navigator.clipboard.writeText(text);
    setCopiedHash(key);
    setTimeout(() => setCopiedHash(null), 2000);
  };

  return (
    <div style={{ flex: 1, padding: '24px', overflowY: 'auto', background: '#000000', display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header & Chain Verification Banner */}
      <div style={{
        background: '#09090b',
        border: '1px solid var(--border-subtle)',
        borderRadius: '14px',
        padding: '20px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '16px'
      }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <h2 className="font-display" style={{ fontSize: '1.25rem', fontWeight: 800, color: '#ffffff' }}>
              Tamper-Evident Cryptographic Audit Ledger
            </h2>
            <span className="badge badge-outline font-mono" style={{ fontSize: '0.65rem' }}>
              CHAINED SHA-256 • TAMPER-EVIDENT
            </span>
          </div>
          <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            Every state mutation, AI inference, and operator resolution is linked in a forward-secure SHA-256 cryptographic sequence.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            onClick={handleVerifyChain}
            disabled={verifying}
            className="btn btn-white btn-sm"
            style={{ fontSize: '0.78rem', padding: '8px 16px' }}
          >
            <ShieldCheck size={14} style={{ color: '#10b981' }} />
            {verifying ? 'Verifying Hashes...' : 'Verify Cryptographic Chain'}
          </button>
        </div>
      </div>

      {/* Verification Result Callout */}
      {verificationResult && (
        <div style={{
          background: verificationResult.isValid ? 'rgba(16, 185, 129, 0.08)' : 'rgba(239, 68, 68, 0.08)',
          border: `1px solid ${verificationResult.isValid ? 'rgba(16, 185, 129, 0.25)' : 'rgba(239, 68, 68, 0.25)'}`,
          borderRadius: '10px',
          padding: '14px 18px',
          display: 'flex',
          alignItems: 'flex-start',
          gap: '12px'
        }}>
          {verificationResult.isValid ? (
            <CheckCircle2 size={20} style={{ color: '#10b981', flexShrink: 0, marginTop: '2px' }} />
          ) : (
            <AlertTriangle size={20} style={{ color: '#ef4444', flexShrink: 0, marginTop: '2px' }} />
          )}

          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2px' }}>
              <span className="font-display" style={{
                fontSize: '0.88rem',
                fontWeight: 800,
                color: verificationResult.isValid ? '#10b981' : '#ef4444'
              }}>
                {verificationResult.isValid ? 'AUDIT CHAIN INTEGRITY: 100% UNBROKEN' : 'CHAIN COMPROMISE / TAMPERING DETECTED'}
              </span>
              <span className="badge badge-outline font-mono" style={{ fontSize: '0.65rem' }}>
                {verificationResult.totalEventsVerified} BLOCKS VERIFIED
              </span>
            </div>

            <div style={{ fontSize: '0.78rem', color: '#d4d4d8', lineHeight: 1.45 }}>
              {verificationResult.verificationSummary}
            </div>

            {verificationResult.latestHeadHash && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '6px', fontSize: '0.7rem', color: 'var(--text-dim)' }}>
                <span>Head Hash:</span>
                <span className="font-mono" style={{ color: '#ffffff' }}>
                  {verificationResult.latestHeadHash.substring(0, 24)}...
                </span>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Search Toolbar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ fontSize: '0.78rem', color: 'var(--text-dim)', fontWeight: 600 }}>
          Displaying {filteredEvents.length} sequential audit records
        </div>

        <div style={{ position: 'relative', width: '280px' }}>
          <Search size={14} style={{ position: 'absolute', left: '10px', top: '10px', color: 'var(--text-dim)' }} />
          <input
            type="text"
            placeholder="Search action, actor, entity..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: '100%',
              padding: '7px 12px 7px 32px',
              background: '#0a0a0a',
              border: '1px solid var(--border-subtle)',
              borderRadius: '6px',
              color: '#ffffff',
              fontSize: '0.78rem',
              outline: 'none',
              fontFamily: 'var(--font-mono)'
            }}
          />
        </div>
      </div>

      {/* Ledger Table */}
      <div className="surface-card" style={{ background: '#060606', overflow: 'hidden', borderRadius: '10px', border: '1px solid var(--border-subtle)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.8rem' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-subtle)', background: '#0c0c0c', color: 'var(--text-muted)' }}>
              <th style={{ padding: '10px 14px', fontWeight: 700, fontSize: '0.68rem', textTransform: 'uppercase', width: '30px' }}></th>
              <th style={{ padding: '10px 14px', fontWeight: 700, fontSize: '0.68rem', textTransform: 'uppercase', width: '60px' }}>Seq</th>
              <th style={{ padding: '10px 14px', fontWeight: 700, fontSize: '0.68rem', textTransform: 'uppercase' }}>Timestamp</th>
              <th style={{ padding: '10px 14px', fontWeight: 700, fontSize: '0.68rem', textTransform: 'uppercase' }}>Action</th>
              <th style={{ padding: '10px 14px', fontWeight: 700, fontSize: '0.68rem', textTransform: 'uppercase' }}>Target Entity</th>
              <th style={{ padding: '10px 14px', fontWeight: 700, fontSize: '0.68rem', textTransform: 'uppercase' }}>Actor</th>
              <th style={{ padding: '10px 14px', fontWeight: 700, fontSize: '0.68rem', textTransform: 'uppercase' }}>SHA-256 Hash</th>
            </tr>
          </thead>
          <tbody>
            {filteredEvents.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-dim)' }}>
                  No audit events found matching search query.
                </td>
              </tr>
            ) : (
              filteredEvents.map((ev, idx) => {
                const isExpanded = expandedId === (ev.auditId || ev.id || idx);
                const currentId = ev.auditId || ev.id || idx;
                const seq = ev.sequenceNumber || idx + 1;
                const hash = ev.currentEventHash || 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';

                return (
                  <React.Fragment key={currentId}>
                    <tr
                      onClick={() => toggleExpand(currentId)}
                      style={{
                        borderBottom: '1px solid var(--border-ultra-subtle)',
                        cursor: 'pointer',
                        background: isExpanded ? 'rgba(255, 255, 255, 0.04)' : 'transparent',
                        transition: 'background 0.15s ease'
                      }}
                    >
                      <td style={{ padding: '10px 14px', color: 'var(--text-dim)' }}>
                        {isExpanded ? <ChevronDown size={13} /> : <ChevronRight size={13} />}
                      </td>
                      <td className="font-mono" style={{ padding: '10px 14px', color: '#06b6d4', fontWeight: 700 }}>
                        #{seq}
                      </td>
                      <td className="font-mono" style={{ padding: '10px 14px', color: 'var(--text-muted)' }}>
                        {ev.createdAt ? new Date(ev.createdAt).toLocaleTimeString() : '14:32:45'}
                      </td>
                      <td style={{ padding: '10px 14px' }}>
                        <span className="badge font-mono" style={{
                          fontSize: '0.68rem',
                          background: 'rgba(255, 255, 255, 0.06)',
                          color: '#ffffff'
                        }}>
                          {ev.action}
                        </span>
                      </td>
                      <td className="font-mono" style={{ padding: '10px 14px', color: '#ffffff' }}>
                        {ev.entityName || ev.entityType}: {ev.entityId}
                      </td>
                      <td style={{ padding: '10px 14px', color: 'var(--text-muted)' }}>
                        {ev.actorId} ({ev.actorType})
                      </td>
                      <td className="font-mono" style={{ padding: '10px 14px', color: '#10b981', fontSize: '0.72rem' }}>
                        {hash.substring(0, 12)}...
                      </td>
                    </tr>

                    {isExpanded && (
                      <tr style={{ background: '#020202', borderBottom: '1px solid var(--border-subtle)' }}>
                        <td colSpan={7} style={{ padding: '16px 20px' }}>
                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '12px' }}>
                            <div>
                              <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
                                Previous Block Hash (Link):
                              </div>
                              <div className="font-mono" style={{ fontSize: '0.72rem', color: 'var(--text-muted)', background: '#080808', padding: '6px 8px', borderRadius: '4px' }}>
                                {ev.previousEventHash || '0000000000000000000000000000000000000000000000000000000000000000'}
                              </div>
                            </div>
                            <div>
                              <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
                                Current Block Hash (SHA-256 Digest):
                              </div>
                              <div className="font-mono" style={{ fontSize: '0.72rem', color: '#10b981', background: '#080808', padding: '6px 8px', borderRadius: '4px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                <span>{hash}</span>
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    copyToClipboard(hash, currentId);
                                  }}
                                  style={{ background: 'transparent', border: 'none', color: '#ffffff', cursor: 'pointer', padding: '2px' }}
                                >
                                  <Copy size={12} />
                                </button>
                              </div>
                            </div>
                          </div>

                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                            <div>
                              <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
                                Previous State
                              </div>
                              <pre className="font-mono" style={{ fontSize: '0.72rem', color: '#d4d4d8', background: '#080808', padding: '8px', borderRadius: '4px', margin: 0, whiteSpace: 'pre-wrap' }}>
                                {ev.previousState || '{}'}
                              </pre>
                            </div>
                            <div>
                              <div style={{ fontSize: '0.68rem', color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
                                Mutated New State
                              </div>
                              <pre className="font-mono" style={{ fontSize: '0.72rem', color: '#d4d4d8', background: '#080808', padding: '8px', borderRadius: '4px', margin: 0, whiteSpace: 'pre-wrap' }}>
                                {ev.newState || '{}'}
                              </pre>
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })
            )}
          </tbody>
        </table>
      </div>

    </div>
  );
}
