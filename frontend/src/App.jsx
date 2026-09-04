import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import LandingPage from './components/LandingPage';
import LoginScreen from './components/LoginScreen';
import CommandCenterView from './components/CommandCenterView';
import PaymentSearchView from './components/PaymentSearchView';
import PaymentInvestigationView from './components/PaymentInvestigationView';
import TimelineView from './components/TimelineView';
import EvidenceVaultView from './components/EvidenceVaultView';
import AiConclusionView from './components/AiConclusionView';
import ResolutionCenterView from './components/ResolutionCenterView';
import SystemHealthView from './components/SystemHealthView';
import AuditLedgerView from './components/AuditLedgerView';
import ActionCenterModal from './components/ActionCenterModal';
import ScenarioSimulator from './components/ScenarioSimulator';
import AiAssessmentModal from './components/AiAssessmentModal';
import CaseQueue from './components/CaseQueue';
import ErrorBanner from './components/ErrorBanner';
import { api } from './services/api';

export default function App() {
  // Navigation: 'landing' | 'login' | 'workstation'
  const [currentPage, setCurrentPage] = useState('landing');
  
  // Workstation Views:
  // 'command_center' | 'investigation' | 'search' | 'timeline' | 'evidence' | 'ai_conclusion' | 'resolution' | 'system_health' | 'audit'
  const [activeView, setActiveView] = useState('investigation');

  const [currentUser, setCurrentUser] = useState({
    id: 'operator_priya_m',
    name: 'Priya Mukherjee',
    role: 'Lead Forensic Investigator',
    clearance: 'Tier-3 Authoritative',
    avatar: 'PM'
  });

  const [cases, setCases] = useState([]);
  const [selectedCaseId, setSelectedCaseId] = useState(null);
  const [selectedCaseDetail, setSelectedCaseDetail] = useState(null);
  const [stats, setStats] = useState(null);
  const [auditEvents, setAuditEvents] = useState([]);
  const [systemHealth, setSystemHealth] = useState(null);

  // Errors & Loaders & Modals
  const [globalError, setGlobalError] = useState(null);
  const [investigationError, setInvestigationError] = useState(null);
  const [loadingCases, setLoadingCases] = useState(false);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [reanalyzing, setReanalyzing] = useState(false);
  const [isActionModalOpen, setIsActionModalOpen] = useState(false);
  const [isSimulatorOpen, setIsSimulatorOpen] = useState(false);
  const [isAiModalOpen, setIsAiModalOpen] = useState(false);

  const [isLastKnownData, setIsLastKnownData] = useState(false);

  useEffect(() => {
    loadAllData();
    const interval = setInterval(loadSystemHealth, 15000);
    return () => clearInterval(interval);
  }, []);

  const loadSystemHealth = async () => {
    try {
      const h = await api.getSystemHealth();
      setSystemHealth(h);
    } catch (e) {
      console.error('System health error:', e);
    }
  };

  const loadAllData = async () => {
    setLoadingCases(true);
    try {
      const [casesData, statsData, auditData, healthData] = await Promise.allSettled([
        api.getIncidents(),
        api.getDashboardSummary(),
        api.getAuditEvents(),
        api.getSystemHealth()
      ]);

      if (casesData.status === 'fulfilled' && casesData.value && casesData.value.length > 0) {
        setCases(casesData.value);
        setGlobalError(null);
        setIsLastKnownData(false);
        const firstId = casesData.value[0].incidentId || casesData.value[0].id;
        setSelectedCaseId(firstId);
        loadCaseDetail(firstId);
      } else {
        if (casesData.status === 'rejected') {
          setCases([]);
          setSelectedCaseId(null);
          setSelectedCaseDetail(null);
          setGlobalError({
            title: 'BACKEND OFFLINE',
            humanMessage: 'Unable to connect to the payment investigation backend server (http://localhost:8080). Check your backend service status.',
            technicalDetails: casesData.reason ? (casesData.reason.humanMessage || casesData.reason.message) : 'Connection refused to backend port 8080',
            endpoint: casesData.reason?.endpoint || '/api/incidents'
          });
          setIsLastKnownData(false);
        }
      }

      if (statsData.status === 'fulfilled' && statsData.value) {
        setStats(statsData.value);
      }
      if (auditData.status === 'fulfilled' && auditData.value) {
        setAuditEvents(auditData.value);
      }
      if (healthData.status === 'fulfilled' && healthData.value) {
        setSystemHealth(healthData.value);
      }
    } catch (e) {
      console.error('Failed to load dashboard data:', e);
      setCases([]);
      setSelectedCaseId(null);
      setSelectedCaseDetail(null);
      setGlobalError({
        title: 'BACKEND OFFLINE',
        humanMessage: 'Unable to connect to the payment investigation backend server (http://localhost:8080). Check your backend service status.',
        technicalDetails: e.humanMessage || e.message,
        endpoint: e.endpoint || '/api'
      });
      setIsLastKnownData(false);
    } finally {
      setLoadingCases(false);
    }
  };

  const loadCaseDetail = async (caseId) => {
    if (!caseId) return;
    setLoadingDetail(true);
    setInvestigationError(null);
    try {
      const detail = await api.getIncidentById(caseId);
      setSelectedCaseDetail(detail);
      setIsLastKnownData(false);
    } catch (e) {
      console.warn(`Failed to load case ${caseId} from server:`, e);
      setInvestigationError(e);
    } finally {
      setLoadingDetail(false);
    }
  };

  const selectCase = (caseId) => {
    setSelectedCaseId(caseId);
    loadCaseDetail(caseId);
    if (activeView === 'command_center' || activeView === 'search') {
      setActiveView('investigation');
    }
  };

  const handleTriggerReanalysis = () => {
    setIsAiModalOpen(true);
  };

  const handleQuickLogin = (personaId) => {
    if (personaId === 'operator_priya_m') {
      setCurrentUser({
        id: 'operator_priya_m',
        name: 'Priya Mukherjee',
        role: 'Lead Forensic Investigator',
        clearance: 'Tier-3 Authoritative',
        avatar: 'PM'
      });
    } else if (personaId === 'risk_lead_vikram') {
      setCurrentUser({
        id: 'risk_lead_vikram',
        name: 'Vikram Singhania',
        role: 'Head of Merchant Risk',
        clearance: 'Executive Clearance',
        avatar: 'VS'
      });
    } else {
      setCurrentUser({
        id: 'settlement_auditor_ananya',
        name: 'Ananya Roy',
        role: 'Fintech Settlement Auditor',
        clearance: 'Audit & Compliance',
        avatar: 'AR'
      });
    }
    setCurrentPage('workstation');
    setActiveView('investigation');
  };

  // Screen 1: LANDING
  if (currentPage === 'landing') {
    return (
      <LandingPage
        onLaunchApp={() => setCurrentPage('workstation')}
        onQuickLogin={handleQuickLogin}
      />
    );
  }

  // Screen 2: LOGIN
  if (currentPage === 'login') {
    return (
      <LoginScreen
        onLogin={(user) => {
          setCurrentUser(user);
          setCurrentPage('workstation');
          setActiveView('investigation');
        }}
        onBackToLanding={() => setCurrentPage('landing')}
      />
    );
  }

  return (
    <div className="app-container">
      {/* Header with 9-view global navigation */}
      <Header
        systemHealth={systemHealth}
        currentUser={currentUser}
        onRefresh={loadAllData}
        onOpenSimulator={() => setIsSimulatorOpen(true)}
        activeView={activeView}
        setActiveView={setActiveView}
        onLogout={() => setCurrentPage('login')}
        onBackToLanding={() => setCurrentPage('landing')}
      />

      {/* Global Error Notice if backend offline */}
      {globalError && (
        <div style={{ padding: '16px 24px 0 24px' }}>
          <ErrorBanner
            error={globalError}
            onRetry={loadAllData}
            onDismiss={() => setGlobalError(null)}
            title={globalError.title || "BACKEND OFFLINE"}
          />
        </div>
      )}

      {/* Last Known Data Indicator */}
      {isLastKnownData && !globalError && (
        <div style={{ padding: '8px 24px 0 24px' }}>
          <div style={{
            background: 'rgba(245, 158, 11, 0.1)',
            border: '1px solid rgba(245, 158, 11, 0.3)',
            borderRadius: '6px',
            padding: '6px 12px',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '0.75rem',
            color: '#fcd34d',
            fontWeight: 600
          }}>
            <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#f59e0b' }} />
            Last known data — not live
          </div>
        </div>
      )}

      {/* Main Workstation View Area */}
      <div className="main-content">
        {/* Render Active Screen */}
        {activeView === 'command_center' && (
          <CommandCenterView
            stats={stats}
            cases={cases}
            onSelectCase={selectCase}
            onNavigateToSearch={() => setActiveView('search')}
            onNavigateToHealth={() => setActiveView('system_health')}
          />
        )}

        {activeView === 'search' && (
          <PaymentSearchView
            cases={cases}
            onSelectCase={selectCase}
          />
        )}

        {activeView === 'investigation' && (
          <div style={{ display: 'flex', width: '100%', height: '100%', overflow: 'hidden' }}>
            {/* Left Case Queue Sidebar */}
            <div style={{ width: '290px', borderRight: '1px solid var(--border-subtle)', height: '100%', overflowY: 'auto', flexShrink: 0 }}>
              <CaseQueue
                cases={cases}
                selectedCaseId={selectedCaseId}
                onSelectCase={selectCase}
                loading={loadingCases}
                onRetry={loadAllData}
              />
            </div>

            {/* Core Workstation View: "What Happened To My Money?" */}
            <div style={{ flex: 1, height: '100%', overflowY: 'auto' }}>
              <PaymentInvestigationView
                incident={selectedCaseDetail}
                onOpenResolution={() => setIsActionModalOpen(true)}
                onTriggerReanalysis={handleTriggerReanalysis}
                reanalyzing={reanalyzing}
                loading={loadingDetail}
                error={investigationError}
                onRetry={() => loadCaseDetail(selectedCaseId)}
              />
            </div>
          </div>
        )}

        {activeView === 'timeline' && (
          <div style={{ display: 'flex', width: '100%', height: '100%', overflow: 'hidden' }}>
            <div style={{ width: '290px', borderRight: '1px solid var(--border-subtle)', height: '100%', overflowY: 'auto', flexShrink: 0 }}>
              <CaseQueue
                cases={cases}
                selectedCaseId={selectedCaseId}
                onSelectCase={selectCase}
                loading={loadingCases}
              />
            </div>
            <div style={{ flex: 1, height: '100%', overflowY: 'auto' }}>
              <TimelineView incident={selectedCaseDetail} />
            </div>
          </div>
        )}

        {activeView === 'evidence' && (
          <div style={{ display: 'flex', width: '100%', height: '100%', overflow: 'hidden' }}>
            <div style={{ width: '290px', borderRight: '1px solid var(--border-subtle)', height: '100%', overflowY: 'auto', flexShrink: 0 }}>
              <CaseQueue
                cases={cases}
                selectedCaseId={selectedCaseId}
                onSelectCase={selectCase}
                loading={loadingCases}
              />
            </div>
            <div style={{ flex: 1, height: '100%', overflowY: 'auto' }}>
              <EvidenceVaultView incident={selectedCaseDetail} />
            </div>
          </div>
        )}

        {activeView === 'ai_conclusion' && (
          <div style={{ display: 'flex', width: '100%', height: '100%', overflow: 'hidden' }}>
            <div style={{ width: '290px', borderRight: '1px solid var(--border-subtle)', height: '100%', overflowY: 'auto', flexShrink: 0 }}>
              <CaseQueue
                cases={cases}
                selectedCaseId={selectedCaseId}
                onSelectCase={selectCase}
                loading={loadingCases}
              />
            </div>
            <div style={{ flex: 1, height: '100%', overflowY: 'auto' }}>
              <AiConclusionView
                incident={selectedCaseDetail}
                onOpenResolution={() => setIsActionModalOpen(true)}
                onTriggerReanalysis={handleTriggerReanalysis}
                reanalyzing={reanalyzing}
              />
            </div>
          </div>
        )}

        {activeView === 'resolution' && (
          <div style={{ display: 'flex', width: '100%', height: '100%', overflow: 'hidden' }}>
            <div style={{ width: '290px', borderRight: '1px solid var(--border-subtle)', height: '100%', overflowY: 'auto', flexShrink: 0 }}>
              <CaseQueue
                cases={cases}
                selectedCaseId={selectedCaseId}
                onSelectCase={selectCase}
                loading={loadingCases}
              />
            </div>
            <div style={{ flex: 1, height: '100%', overflowY: 'auto' }}>
              <ResolutionCenterView
                incident={selectedCaseDetail}
                currentUser={currentUser}
                onResolved={() => loadAllData()}
              />
            </div>
          </div>
        )}

        {activeView === 'system_health' && (
          <SystemHealthView />
        )}

        {activeView === 'audit' && (
          <div style={{ flex: 1, height: '100%', overflowY: 'auto', padding: '24px' }}>
            <AuditLedgerView
              auditEvents={auditEvents}
              onRefresh={loadAllData}
            />
          </div>
        )}
      </div>

      {/* Action Resolution Modal */}
      {isActionModalOpen && selectedCaseDetail && (
        <ActionCenterModal
          isOpen={isActionModalOpen}
          onClose={() => setIsActionModalOpen(false)}
          incident={selectedCaseDetail}
          currentUser={currentUser}
          onResolved={() => {
            setIsActionModalOpen(false);
            loadAllData();
          }}
        />
      )}

      {/* Live AI Forensic Inference Modal */}
      {isAiModalOpen && selectedCaseDetail && (
        <AiAssessmentModal
          isOpen={isAiModalOpen}
          onClose={() => setIsAiModalOpen(false)}
          incident={selectedCaseDetail}
          onOpenResolution={() => {
            setIsAiModalOpen(false);
            setIsActionModalOpen(true);
          }}
        />
      )}

      {/* Anomaly Simulator Modal */}
      {isSimulatorOpen && (
        <ScenarioSimulator
          isOpen={isSimulatorOpen}
          onClose={() => setIsSimulatorOpen(false)}
          onSimulationComplete={() => {
            setIsSimulatorOpen(false);
            loadAllData();
          }}
        />
      )}
    </div>
  );
}
