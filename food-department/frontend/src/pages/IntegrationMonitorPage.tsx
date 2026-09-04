import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { govmeshService } from '../services/govmeshService';
import { IntegrationTransaction, CanonicalAddressUpdateRequest } from '../types/integration';
import { Breadcrumb } from '../components/common/Breadcrumb';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorState } from '../components/common/ErrorState';
import { EmptyState } from '../components/common/EmptyState';
import { formatDate } from '../utils/formatters';
import { Network, Play, RefreshCw, Eye, CheckCircle2, XCircle, ArrowRight, ShieldCheck, AlertTriangle, Layers, Copy } from 'lucide-react';

export const IntegrationMonitorPage: React.FC = () => {
  const [transactions, setTransactions] = useState<IntegrationTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [simulating, setSimulating] = useState(false);
  const [simMode, setSimMode] = useState<string>('SUCCESS');
  const [simMsg, setSimMsg] = useState<{ type: 'success' | 'blocked' | 'retrying' | 'error'; text: string } | null>(null);

  const fetchTransactions = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await govmeshService.getTransactions();
      setTransactions(data);
      const modeObj = await govmeshService.getSimulationMode();
      setSimMode(modeObj.mode);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to fetch integration transactions.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  const handleSetSimMode = async (mode: string) => {
    try {
      const res = await govmeshService.setSimulationMode(mode);
      setSimMode(res.mode);
      setSimMsg({
        type: mode === 'SUCCESS' ? 'success' : 'retrying',
        text: `SOAP Failure Simulation Mode set to: ${res.mode}`,
      });
    } catch (err: any) {
      setError('Failed to set SOAP simulation mode.');
    }
  };

  const handleSimulateRequest = async (scenario: 'valid' | 'duplicate' | 'expired' | 'revoked' | 'purpose' | 'field') => {
    setSimulating(true);
    setSimMsg(null);
    setError(null);

    let reqPayload: CanonicalAddressUpdateRequest;

    const baseCitizen = {
      reference: 'CIT-MH-998811',
      name: 'Rajesh Kumar',
      address: {
        line: '44 Example Road, Shivajinagar, Pune - 411005',
        district: 'DIST-PUN',
        taluka: 'TAL-PUN-04',
      },
    };

    const baseVerification = {
      status: 'VALID',
      source: 'REVENUE',
    };

    if (scenario === 'valid') {
      const randomAppId = `GM-2026-${Math.floor(100000 + Math.random() * 900000)}`;
      reqPayload = {
        applicationId: randomAppId,
        sourceDepartment: 'REVENUE',
        targetDepartment: 'FOOD',
        correlationId: `REQ-2026-${Math.floor(100000 + Math.random() * 900000)}`,
        idempotencyKey: `${randomAppId}:ADDRESS_UPDATE`,
        purpose: 'RATION_ADDRESS_UPDATE',
        requestedFields: ['citizen.name', 'citizen.address', 'citizen.address.district', 'citizen.address.taluka', 'verification.status'],
        citizen: baseCitizen,
        verification: baseVerification,
        consent: { id: 'CONSENT-00124' },
      };
    } else if (scenario === 'duplicate') {
      reqPayload = {
        applicationId: 'GM-2026-000124',
        sourceDepartment: 'REVENUE',
        targetDepartment: 'FOOD',
        correlationId: `REQ-2026-${Math.floor(100000 + Math.random() * 900000)}`,
        idempotencyKey: 'GM-2026-000124:ADDRESS_UPDATE',
        purpose: 'RATION_ADDRESS_UPDATE',
        requestedFields: ['citizen.name', 'citizen.address', 'citizen.address.district', 'citizen.address.taluka', 'verification.status'],
        citizen: baseCitizen,
        verification: baseVerification,
        consent: { id: 'CONSENT-00124' },
      };
    } else if (scenario === 'expired') {
      reqPayload = {
        applicationId: 'GM-2026-000124',
        sourceDepartment: 'REVENUE',
        targetDepartment: 'FOOD',
        correlationId: `REQ-2026-${Math.floor(100000 + Math.random() * 900000)}`,
        idempotencyKey: `GM-2026-EXPIRED:${Math.floor(100000 + Math.random() * 900000)}`,
        purpose: 'RATION_ADDRESS_UPDATE',
        requestedFields: ['citizen.name', 'citizen.address'],
        citizen: baseCitizen,
        verification: baseVerification,
        consent: { id: 'CONSENT-EXPIRED-001' },
      };
    } else if (scenario === 'revoked') {
      reqPayload = {
        applicationId: 'GM-2026-000124',
        sourceDepartment: 'REVENUE',
        targetDepartment: 'FOOD',
        correlationId: `REQ-2026-${Math.floor(100000 + Math.random() * 900000)}`,
        idempotencyKey: `GM-2026-REVOKED:${Math.floor(100000 + Math.random() * 900000)}`,
        purpose: 'RATION_ADDRESS_UPDATE',
        requestedFields: ['citizen.name', 'citizen.address'],
        citizen: baseCitizen,
        verification: baseVerification,
        consent: { id: 'CONSENT-REVOKED-001' },
      };
    } else if (scenario === 'purpose') {
      reqPayload = {
        applicationId: 'GM-2026-000124',
        sourceDepartment: 'REVENUE',
        targetDepartment: 'FOOD',
        correlationId: `REQ-2026-${Math.floor(100000 + Math.random() * 900000)}`,
        idempotencyKey: `GM-2026-WRONGPURPOSE:${Math.floor(100000 + Math.random() * 900000)}`,
        purpose: 'LOAN_APPLICATION',
        requestedFields: ['citizen.name', 'citizen.address'],
        citizen: baseCitizen,
        verification: baseVerification,
        consent: { id: 'CONSENT-00124' },
      };
    } else {
      // field
      reqPayload = {
        applicationId: 'GM-2026-000124',
        sourceDepartment: 'REVENUE',
        targetDepartment: 'FOOD',
        correlationId: `REQ-2026-${Math.floor(100000 + Math.random() * 900000)}`,
        idempotencyKey: `GM-2026-FIELDVIOLATION:${Math.floor(100000 + Math.random() * 900000)}`,
        purpose: 'RATION_ADDRESS_UPDATE',
        requestedFields: ['citizen.name', 'citizen.address', 'citizen.phone'],
        citizen: baseCitizen,
        verification: baseVerification,
        consent: { id: 'CONSENT-00124' },
      };
    }

    try {
      const res = await govmeshService.simulateInteroperability(reqPayload);
      if (res.status === 'BLOCKED') {
        setSimMsg({
          type: 'blocked',
          text: `Request BLOCKED by Consent Gatekeeper! Reason: ${res.errorCode || res.message}. Correlation ID: ${res.correlationId}`,
        });
      } else if (res.status === 'RETRYING') {
        setSimMsg({
          type: 'retrying',
          text: `SOAP Failure Encountered (${res.errorCode})! Request safely queued for background retry. Correlation ID: ${res.correlationId}`,
        });
      } else {
        setSimMsg({
          type: 'success',
          text: `Request Processed: ${res.message} Correlation ID: ${res.correlationId}`,
        });
      }
      await fetchTransactions();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to execute simulated request.');
    } finally {
      setSimulating(false);
    }
  };

  return (
    <div className="space-y-4 text-xs">
      <Breadcrumb items={[{ label: 'System' }, { label: 'Interoperability Monitor' }]} />

      {/* Header Bar */}
      <div className="bg-white p-3.5 rounded border border-slate-300 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-base font-extrabold text-blue-950 flex items-center gap-1.5">
              <Network className="w-5 h-5 text-blue-900" />
              GovMesh Interoperability Monitor
            </h1>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-blue-100 text-blue-900 border border-blue-300 font-mono">
              REST ➔ CONSENT ➔ IDEMPOTENCY ➔ SOAP GATEWAY
            </span>
          </div>
          <p className="text-xs text-slate-600 mt-0.5">
            GovMesh inter-departmental data exchange monitor with reliability, retry tracking, and duplicate detection.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link
            to="/integration/retries"
            className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-amber-900 bg-amber-50 hover:bg-amber-100 border border-amber-300 rounded shadow-xs transition shrink-0"
          >
            <AlertTriangle className="w-3.5 h-3.5 mr-1.5 text-amber-600" />
            Failed & Retry Queue
          </Link>

          <button
            onClick={fetchTransactions}
            className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 border border-slate-300 rounded shadow-xs transition shrink-0"
          >
            <RefreshCw className="w-3.5 h-3.5 mr-1.5 text-slate-500" />
            Refresh
          </button>
        </div>
      </div>

      {/* Development / Demo Controls: Failure Mode & Scenario Trigger */}
      <div className="bg-white rounded border border-slate-300 shadow-xs p-3.5 space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-200 pb-2">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wide flex items-center gap-1.5">
            <Layers className="w-4 h-4 text-blue-900" />
            PHASE 6 DEMO CONTROLS: SOAP FAILURE SIMULATION & IDEMPOTENCY
          </h2>
          <div className="flex items-center gap-1.5 font-mono text-[11px]">
            <span className="text-slate-600 font-bold">SOAP Mode:</span>
            <span className={`px-2 py-0.5 rounded font-extrabold uppercase ${simMode === 'SUCCESS' ? 'bg-emerald-100 text-emerald-900 border border-emerald-300' : 'bg-red-100 text-red-900 border border-red-300'}`}>
              {simMode}
            </span>
          </div>
        </div>

        {/* Failure Mode Toggle Buttons */}
        <div className="flex items-center gap-2">
          <span className="text-[11px] font-bold text-slate-700 uppercase">Set SOAP Mode:</span>
          <button
            onClick={() => handleSetSimMode('SUCCESS')}
            className={`px-2.5 py-1 rounded text-[11px] font-bold transition border ${simMode === 'SUCCESS' ? 'bg-emerald-700 text-white border-emerald-800' : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'}`}
          >
            ✓ SUCCESS
          </button>
          <button
            onClick={() => handleSetSimMode('TIMEOUT')}
            className={`px-2.5 py-1 rounded text-[11px] font-bold transition border ${simMode === 'TIMEOUT' ? 'bg-amber-600 text-white border-amber-700' : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'}`}
          >
            ⏱ TIMEOUT (Simulated)
          </button>
          <button
            onClick={() => handleSetSimMode('SERVICE_UNAVAILABLE')}
            className={`px-2.5 py-1 rounded text-[11px] font-bold transition border ${simMode === 'SERVICE_UNAVAILABLE' ? 'bg-red-700 text-white border-red-800' : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'}`}
          >
            🚫 SERVICE_UNAVAILABLE (Simulated)
          </button>
        </div>

        {/* Trigger Scenarios */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-2 pt-1 border-t border-slate-200">
          <button
            onClick={() => handleSimulateRequest('valid')}
            disabled={simulating}
            className="px-2.5 py-2 rounded text-[11px] font-bold bg-emerald-700 hover:bg-emerald-800 text-white flex items-center justify-center gap-1 transition shadow-xs"
          >
            <Play className="w-3 h-3 text-amber-300" />
            1. New Request
          </button>

          <button
            onClick={() => handleSimulateRequest('duplicate')}
            disabled={simulating}
            className="px-2.5 py-2 rounded text-[11px] font-bold bg-blue-900 hover:bg-blue-950 text-white flex items-center justify-center gap-1 transition shadow-xs"
          >
            <Copy className="w-3 h-3 text-amber-400" />
            2. Duplicate Req
          </button>

          <button
            onClick={() => handleSimulateRequest('expired')}
            disabled={simulating}
            className="px-2.5 py-2 rounded text-[11px] font-bold bg-amber-600 hover:bg-amber-700 text-white flex items-center justify-center gap-1 transition shadow-xs"
          >
            <Play className="w-3 h-3 text-white" />
            3. Expired Consent
          </button>

          <button
            onClick={() => handleSimulateRequest('revoked')}
            disabled={simulating}
            className="px-2.5 py-2 rounded text-[11px] font-bold bg-red-700 hover:bg-red-800 text-white flex items-center justify-center gap-1 transition shadow-xs"
          >
            <Play className="w-3 h-3 text-white" />
            4. Revoked Consent
          </button>

          <button
            onClick={() => handleSimulateRequest('purpose')}
            disabled={simulating}
            className="px-2.5 py-2 rounded text-[11px] font-bold bg-purple-700 hover:bg-purple-800 text-white flex items-center justify-center gap-1 transition shadow-xs"
          >
            <Play className="w-3 h-3 text-white" />
            5. Wrong Purpose
          </button>

          <button
            onClick={() => handleSimulateRequest('field')}
            disabled={simulating}
            className="px-2.5 py-2 rounded text-[11px] font-bold bg-slate-700 hover:bg-slate-800 text-white flex items-center justify-center gap-1 transition shadow-xs"
          >
            <Play className="w-3 h-3 text-white" />
            6. Field Violation
          </button>
        </div>
      </div>

      {simMsg && (
        <div
          className={`p-3 rounded text-xs flex items-center justify-between border ${
            simMsg.type === 'success'
              ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
              : simMsg.type === 'retrying'
              ? 'bg-amber-50 border-amber-300 text-amber-900'
              : simMsg.type === 'blocked'
              ? 'bg-red-50 border-red-300 text-red-900'
              : 'bg-slate-100 border-slate-300 text-slate-900'
          }`}
        >
          <div className="flex items-center gap-2">
            {simMsg.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 text-emerald-700 shrink-0" />
            ) : simMsg.type === 'retrying' ? (
              <AlertTriangle className="w-4 h-4 text-amber-700 shrink-0" />
            ) : (
              <XCircle className="w-4 h-4 text-red-700 shrink-0" />
            )}
            <span className="font-bold">{simMsg.text}</span>
          </div>
          <button onClick={() => setSimMsg(null)} className="font-bold text-xs underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Main Integration Transactions Table with Phase 6 Columns */}
      {loading ? (
        <LoadingState message="Fetching GovMesh integration activity records..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchTransactions} />
      ) : transactions.length === 0 ? (
        <EmptyState
          title="No Integration Transactions Recorded"
          description="Use scenario buttons above to simulate inter-departmental requests."
        />
      ) : (
        <div className="bg-white rounded border border-slate-300 shadow-xs overflow-hidden space-y-2">
          <div className="p-3 border-b border-slate-200 flex justify-between items-center bg-slate-50">
            <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wide">
              INTEROPERABILITY TRANSACTIONS & RELIABILITY MONITOR
            </h2>
            <span className="text-[10px] text-slate-500 font-mono">Total Traces: {transactions.length}</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-900 text-slate-200 uppercase font-bold tracking-wider border-b border-slate-800 text-[10px]">
                  <th className="py-2.5 px-3 border-r border-slate-800">Application</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Operation</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Status</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Attempts</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Last Attempt</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Next Retry</th>
                  <th className="py-2.5 px-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-slate-50 transition">
                    <td className="py-2 px-3 font-mono font-bold text-slate-900">
                      {tx.applicationId}
                      <span className="text-[10px] text-slate-500 font-mono block">{tx.correlationId}</span>
                    </td>
                    <td className="py-2 px-3 font-semibold text-slate-800">
                      {tx.operation || 'Address Update'}
                      <span className="text-[10px] text-slate-500 font-mono block">
                        {tx.sourceDepartment} <ArrowRight className="w-2.5 h-2.5 inline text-slate-400" /> {tx.targetDepartment}
                      </span>
                    </td>
                    <td className="py-2 px-3">
                      <StatusBadge status={tx.retryStatus || tx.status} />
                    </td>
                    <td className="py-2 px-3 font-mono font-bold text-slate-800">
                      {tx.attemptCount || 1} / {tx.maxAttempts || 3}
                    </td>
                    <td className="py-2 px-3 text-slate-600 font-medium">
                      {formatDate(tx.lastAttemptAt || tx.startedAt)}
                    </td>
                    <td className="py-2 px-3 font-mono text-slate-700">
                      {tx.nextRetryAt ? (
                        <span className="px-1.5 py-0.5 bg-amber-50 text-amber-900 border border-amber-300 rounded font-bold">
                          {formatDate(tx.nextRetryAt)}
                        </span>
                      ) : (
                        <span className="text-slate-400">—</span>
                      )}
                    </td>
                    <td className="py-2 px-3 text-right">
                      <Link
                        to={`/integration/${tx.correlationId}`}
                        className="inline-flex items-center px-2 py-0.5 text-[11px] font-semibold text-blue-900 bg-blue-50 hover:bg-blue-100 border border-blue-300 rounded transition"
                      >
                        <Eye className="w-3 h-3 mr-1" />
                        View Trace
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
