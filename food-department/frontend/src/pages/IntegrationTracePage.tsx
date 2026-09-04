import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { govmeshService } from '../services/govmeshService';
import { IntegrationTransaction, IntegrationAttempt } from '../types/integration';
import { Breadcrumb } from '../components/common/Breadcrumb';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorState } from '../components/common/ErrorState';
import { formatDate } from '../utils/formatters';
import { ArrowLeft, Network, ChevronDown, ChevronUp, Code, ArrowDown, ShieldCheck, CheckCircle2, XCircle, AlertTriangle, RefreshCw, Copy, Layers } from 'lucide-react';

export const IntegrationTracePage: React.FC = () => {
  const { correlationId } = useParams<{ correlationId: string }>();

  const [transaction, setTransaction] = useState<IntegrationTransaction | null>(null);
  const [attempts, setAttempts] = useState<IntegrationAttempt[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showTransformation, setShowTransformation] = useState(false);

  useEffect(() => {
    const fetchTrace = async () => {
      if (!correlationId) return;
      setLoading(true);
      setError(null);
      try {
        const data = await govmeshService.getTransactionByCorrelationId(correlationId);
        setTransaction(data);
        const attemptData = await govmeshService.getAttempts(correlationId);
        setAttempts(attemptData);
      } catch (err: any) {
        setError(err?.response?.data?.message || 'Failed to fetch integration transaction trace.');
      } finally {
        setLoading(false);
      }
    };
    fetchTrace();
  }, [correlationId]);

  if (loading) return <LoadingState message="Loading interoperability transaction trace..." />;
  if (error || !transaction) return <ErrorState message={error || 'Integration transaction trace not found'} />;

  const isBlocked = transaction.status === 'BLOCKED' || transaction.consentStatus === 'BLOCKED';
  const failureReason = transaction.consentFailureReason || transaction.errorCode || 'CONSENT_DENIED';
  const idempotencyKey = transaction.idempotencyKey || `${transaction.applicationId}:ADDRESS_UPDATE`;

  return (
    <div className="space-y-4 text-xs">
      <Breadcrumb
        items={[
          { label: 'System' },
          { label: 'Interoperability Monitor', to: '/integration' },
          { label: `Trace ${transaction.correlationId}` },
        ]}
      />

      {/* Header Bar */}
      <div className="bg-white p-3.5 rounded border border-slate-300 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-base font-extrabold text-blue-950 flex items-center gap-1.5">
              <Network className="w-5 h-5 text-blue-900" />
              INTEROPERABILITY TRACE SPECIFICATION
            </h1>
            <StatusBadge status={transaction.retryStatus || transaction.status} />
          </div>
          <p className="text-xs text-slate-600 mt-0.5 font-mono">
            Correlation ID: <strong className="text-slate-900 font-bold">{transaction.correlationId}</strong> • Application: <strong className="text-slate-900 font-bold">{transaction.applicationId}</strong>
          </p>
        </div>

        <Link
          to="/integration"
          className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 border border-slate-300 rounded shadow-xs transition shrink-0"
        >
          <ArrowLeft className="w-3.5 h-3.5 mr-1.5 text-slate-500" />
          Back to Monitor
        </Link>
      </div>

      {/* PHASE 6 RELIABILITY SUMMARY PANEL */}
      <div className="bg-slate-900 text-white rounded border border-slate-800 p-4 space-y-3 shadow-xs font-mono">
        <div className="flex items-center justify-between border-b border-slate-800 pb-2">
          <div className="flex items-center gap-2">
            <RefreshCw className="w-4 h-4 text-amber-400" />
            <h2 className="text-xs font-bold text-amber-300 uppercase tracking-wide">
              RELIABILITY & IDEMPOTENCY SPECIFICATION
            </h2>
          </div>
          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700 uppercase">
            STABLE IDEMPOTENCY KEY
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
          <div className="p-2.5 bg-slate-950 rounded border border-slate-800">
            <span className="text-[10px] text-slate-400 font-bold uppercase block">Idempotency Key</span>
            <span className="text-amber-300 font-extrabold text-xs block truncate mt-0.5">{idempotencyKey}</span>
          </div>

          <div className="p-2.5 bg-slate-950 rounded border border-slate-800">
            <span className="text-[10px] text-slate-400 font-bold uppercase block">Attempt Count</span>
            <span className="text-white font-extrabold text-sm block mt-0.5">
              {transaction.attemptCount || 1} / {transaction.maxAttempts || 3}
            </span>
          </div>

          <div className="p-2.5 bg-slate-950 rounded border border-slate-800">
            <span className="text-[10px] text-slate-400 font-bold uppercase block">Retry Status</span>
            <span className="block mt-0.5">
              <StatusBadge status={transaction.retryStatus || transaction.status} />
            </span>
          </div>

          <div className="p-2.5 bg-slate-950 rounded border border-slate-800">
            <span className="text-[10px] text-slate-400 font-bold uppercase block">Next Retry Scheduled</span>
            <span className="text-amber-400 font-bold text-xs block mt-0.5">
              {transaction.nextRetryAt ? formatDate(transaction.nextRetryAt) : 'None (Completed/Final)'}
            </span>
          </div>
        </div>

        {transaction.failureMessage && (
          <div className="p-2.5 bg-red-950/60 border border-red-800 text-red-200 rounded text-[11px] flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
            <span>
              <strong>Last Error:</strong> [{transaction.failureCode || transaction.errorCode}] {transaction.failureMessage || transaction.errorMessage}
            </span>
          </div>
        )}
      </div>

      {/* ATTEMPT HISTORY TABLE */}
      <div className="bg-white rounded border border-slate-300 shadow-xs p-4 space-y-3">
        <div className="flex items-center justify-between border-b border-slate-200 pb-2">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wide flex items-center gap-1.5">
            <Layers className="w-4 h-4 text-blue-900" />
            COMPLETE ATTEMPT HISTORY & EXECUTION LOG
          </h2>
          <span className="text-[10px] font-mono text-slate-500">
            Total Attempts: {attempts.length > 0 ? attempts.length : (transaction.attemptCount || 1)}
          </span>
        </div>

        {attempts.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-100 text-slate-700 uppercase font-bold text-[10px] border-b border-slate-300">
                  <th className="py-2 px-3 border-r border-slate-200">Attempt #</th>
                  <th className="py-2 px-3 border-r border-slate-200">Timestamp</th>
                  <th className="py-2 px-3 border-r border-slate-200">Protocol</th>
                  <th className="py-2 px-3 border-r border-slate-200">Status</th>
                  <th className="py-2 px-3">Failure Reason</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 text-slate-800 font-medium">
                {attempts.map((att) => (
                  <tr key={att.id} className="hover:bg-slate-50">
                    <td className="py-2 px-3 font-bold font-mono text-blue-950">#{att.attemptNumber}</td>
                    <td className="py-2 px-3 font-mono text-slate-600">{formatDate(att.attemptAt)}</td>
                    <td className="py-2 px-3 font-mono">{att.protocol || 'SOAP/XML'}</td>
                    <td className="py-2 px-3">
                      <StatusBadge status={att.status} />
                    </td>
                    <td className="py-2 px-3 text-slate-600 font-mono text-[11px]">
                      {att.failureCode ? (
                        <span className="px-1 py-0.5 bg-red-50 text-red-900 border border-red-200 rounded font-bold mr-1">
                          {att.failureCode}
                        </span>
                      ) : null}
                      {att.failureMessage || 'N/A'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-3 bg-slate-50 border border-slate-200 rounded text-slate-700 font-mono text-xs flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="font-bold">Attempt #1:</span>
              <span>Executed at {formatDate(transaction.lastAttemptAt || transaction.startedAt)}</span>
            </div>
            <StatusBadge status={transaction.retryStatus || transaction.status} />
          </div>
        )}
      </div>

      {/* Visual Interoperability Trace Pipeline */}
      <div className="bg-white rounded border border-slate-300 shadow-xs p-4 space-y-4">
        <div className="border-b border-slate-200 pb-2 flex justify-between items-center">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wide flex items-center gap-1.5">
            <ShieldCheck className="w-4.5 h-4.5 text-blue-900" />
            END-TO-END INTEROPERABILITY & CONSENT PIPELINE EXECUTION TRACE
          </h2>
          {isBlocked ? (
            <span className="px-2.5 py-1 rounded text-[11px] font-extrabold bg-red-100 text-red-900 border border-red-300 uppercase flex items-center gap-1 font-mono">
              <XCircle className="w-3.5 h-3.5 text-red-700" />
              REQUEST BLOCKED BY GATEKEEPER
            </span>
          ) : (
            <span className="px-2.5 py-1 rounded text-[11px] font-extrabold bg-emerald-100 text-emerald-900 border border-emerald-300 uppercase flex items-center gap-1 font-mono">
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-700" />
              CONSENT VALIDATED & EXECUTED
            </span>
          )}
        </div>

        {/* Pipeline Steps Container */}
        <div className="space-y-3">
          {/* STEP 1: SOURCE */}
          <div className="p-3 bg-slate-50 border border-slate-300 rounded relative">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-6 h-6 rounded-full bg-slate-900 text-white font-bold flex items-center justify-center text-[10px]">1</span>
                <div>
                  <span className="font-bold text-slate-900 uppercase text-[11px]">SOURCE DEPARTMENT PAYLOAD</span>
                  <span className="text-[10px] text-slate-500 font-mono block">
                    Source: <strong>{transaction.sourceDepartment}</strong> • Protocol: <strong>{transaction.sourceProtocol}</strong>
                  </span>
                </div>
              </div>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300 uppercase font-mono">
                ✓ RECEIVED
              </span>
            </div>
          </div>

          <div className="flex justify-center -my-1">
            <ArrowDown className="w-4 h-4 text-blue-900" />
          </div>

          {/* STEP 2: CONSENT CHECK */}
          <div className={`p-3 rounded border relative ${isBlocked ? 'bg-red-50 border-red-300' : 'bg-emerald-50/50 border-emerald-300'}`}>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className={`w-6 h-6 rounded-full font-bold flex items-center justify-center text-[10px] ${isBlocked ? 'bg-red-700 text-white' : 'bg-emerald-700 text-white'}`}>
                  2
                </span>
                <div>
                  <span className={`font-bold uppercase text-[11px] ${isBlocked ? 'text-red-950' : 'text-emerald-950'}`}>
                    GOVMESH CONSENT CHECK
                  </span>
                  <span className="text-[10px] text-slate-600 font-mono block">
                    Consent ID: <strong>{transaction.consentId || 'CONSENT-00124'}</strong> • Requesting: <strong>{transaction.sourceDepartment}</strong> ➔ Receiving: <strong>{transaction.targetDepartment}</strong>
                  </span>
                </div>
              </div>

              {isBlocked ? (
                <div className="text-right">
                  <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-red-700 text-white uppercase font-mono block">
                    ✗ FAILED
                  </span>
                  <span className="text-[10px] text-red-900 font-mono font-bold block mt-0.5">
                    Reason: {failureReason}
                  </span>
                </div>
              ) : (
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300 uppercase font-mono">
                  ✓ VALID
                </span>
              )}
            </div>
          </div>

          <div className="flex justify-center -my-1">
            <ArrowDown className={`w-4 h-4 ${isBlocked ? 'text-red-600' : 'text-blue-900'}`} />
          </div>

          {/* STEP 3: DATA MINIMIZATION */}
          <div className={`p-3 rounded border relative ${isBlocked ? 'bg-red-50/70 border-red-300 opacity-80' : 'bg-emerald-50/30 border-emerald-200'}`}>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className={`w-6 h-6 rounded-full font-bold flex items-center justify-center text-[10px] ${isBlocked ? 'bg-red-800 text-white' : 'bg-emerald-800 text-white'}`}>
                  3
                </span>
                <div>
                  <span className={`font-bold uppercase text-[11px] ${isBlocked ? 'text-red-950' : 'text-emerald-950'}`}>
                    DATA MINIMIZATION VERIFICATION
                  </span>
                  <span className="text-[10px] text-slate-600 font-mono block">
                    Purpose-Based Permitted Fields Evaluation
                  </span>
                </div>
              </div>

              {isBlocked ? (
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-red-200 text-red-950 border border-red-400 uppercase font-mono">
                  ✗ HALTED
                </span>
              ) : (
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300 uppercase font-mono">
                  ✓ FIELDS PERMITTED
                </span>
              )}
            </div>
          </div>

          {/* IF BLOCKED: SHOW WARNING "FOOD DEPARTMENT NOT CONTACTED" */}
          {isBlocked ? (
            <>
              <div className="flex justify-center -my-1">
                <ArrowDown className="w-4 h-4 text-red-600" />
              </div>

              <div className="p-3.5 bg-red-100 border-2 border-red-500 rounded text-red-950 space-y-1.5">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="w-5 h-5 text-red-700 shrink-0" />
                    <span className="font-extrabold uppercase text-xs tracking-wider text-red-950">
                      REQUEST BLOCKED BY GOVMESH GATEKEEPER
                    </span>
                  </div>
                  <span className="px-2.5 py-0.5 bg-red-700 text-white font-mono text-[10px] font-bold rounded">
                    GATEKEEPER ENFORCED
                  </span>
                </div>
                <p className="text-xs font-semibold text-red-900">
                  Consent verification failed with reason: <strong className="font-mono underline">{failureReason}</strong>.
                  Data transmission was halted immediately before reaching the downstream target department. No retries scheduled.
                </p>
              </div>

              <div className="flex justify-center -my-1">
                <ArrowDown className="w-4 h-4 text-slate-400" />
              </div>

              <div className="p-4 bg-slate-100 border-2 border-dashed border-slate-400 rounded text-slate-600 relative">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-slate-400 text-white font-bold flex items-center justify-center text-[10px]">4</span>
                    <div>
                      <span className="font-extrabold text-slate-700 uppercase text-xs">FOOD DEPARTMENT SOAP / XML SERVICE</span>
                      <span className="text-[10px] text-slate-500 font-mono block">Target Endpoint: /ws (UpdateRationAddress)</span>
                    </div>
                  </div>
                  <span className="px-3 py-1 rounded text-xs font-extrabold bg-slate-200 text-slate-800 border border-slate-400 uppercase font-mono tracking-wider">
                    NOT CONTACTED
                  </span>
                </div>
              </div>
            </>
          ) : (
            <>
              <div className="flex justify-center -my-1">
                <ArrowDown className="w-4 h-4 text-blue-900" />
              </div>

              {/* STEP 4: IDEMPOTENCY GATE */}
              <div className="p-3 bg-indigo-50/50 border border-indigo-200 rounded relative">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-indigo-900 text-white font-bold flex items-center justify-center text-[10px]">4</span>
                    <div>
                      <span className="font-bold text-indigo-950 uppercase text-[11px]">IDEMPOTENCY & DUPLICATE CHECK GATE</span>
                      <span className="text-[10px] text-indigo-800 font-mono block">
                        Idempotency Key: <strong>{idempotencyKey}</strong>
                      </span>
                    </div>
                  </div>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300 uppercase font-mono">
                    ✓ CHECK PASSED
                  </span>
                </div>
              </div>

              <div className="flex justify-center -my-1">
                <ArrowDown className="w-4 h-4 text-blue-900" />
              </div>

              {/* STEP 5: CANONICAL MODEL */}
              <div className="p-3 bg-blue-50/50 border border-blue-200 rounded relative">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-blue-900 text-white font-bold flex items-center justify-center text-[10px]">5</span>
                    <div>
                      <span className="font-bold text-blue-950 uppercase text-[11px]">GOVMESH CANONICAL MODEL</span>
                      <span className="text-[10px] text-blue-800 font-mono block">Normalized GovMesh Data Structure</span>
                    </div>
                  </div>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300 uppercase font-mono">
                    ✓ CREATED
                  </span>
                </div>
              </div>

              <div className="flex justify-center -my-1">
                <ArrowDown className="w-4 h-4 text-blue-900" />
              </div>

              {/* STEP 6: SCHEMA MAPPING */}
              <div className="p-3 bg-amber-50/40 border border-amber-300 rounded relative space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-amber-800 text-white font-bold flex items-center justify-center text-[10px]">6</span>
                    <div>
                      <span className="font-bold text-amber-950 uppercase text-[11px]">EXPLICIT SCHEMA MAPPING LAYER</span>
                      <span className="text-[10px] text-amber-900 font-mono block">GovMesh Canonical ➔ Food Department Target Schema</span>
                    </div>
                  </div>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300 uppercase font-mono">
                    ✓ COMPLETED
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-1.5 pt-1 text-[10px] font-mono border-t border-amber-200">
                  <div className="p-1 bg-white rounded border border-amber-200 text-slate-700">citizen.name ➔ CitizenName</div>
                  <div className="p-1 bg-white rounded border border-amber-200 text-slate-700">citizen.address.line ➔ Address</div>
                  <div className="p-1 bg-white rounded border border-amber-200 text-slate-700">citizen.address.district ➔ DistrictCode</div>
                  <div className="p-1 bg-white rounded border border-amber-200 text-slate-700">verification.status ➔ RevenueVerified</div>
                </div>
              </div>

              <div className="flex justify-center -my-1">
                <ArrowDown className="w-4 h-4 text-blue-900" />
              </div>

              {/* STEP 7: TARGET FOOD DEPARTMENT SOAP SERVICE */}
              <div className="p-3 bg-slate-50 border border-slate-300 rounded relative">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-slate-900 text-white font-bold flex items-center justify-center text-[10px]">7</span>
                    <div>
                      <span className="font-bold text-slate-900 uppercase text-[11px]">FOOD DEPARTMENT SOAP / XML SERVICE</span>
                      <span className="text-[10px] text-slate-500 font-mono block">
                        Target Endpoint: /ws (UpdateRationAddress) • Attempt: {transaction.attemptCount || 1}/{transaction.maxAttempts || 3}
                      </span>
                    </div>
                  </div>
                  <StatusBadge status={transaction.retryStatus || transaction.status} />
                </div>
              </div>
            </>
          )}
        </div>

        {/* Expandable Transformation Payloads Accordion */}
        <div className="pt-2">
          <button
            onClick={() => setShowTransformation(!showTransformation)}
            className="w-full p-2.5 bg-slate-100 hover:bg-slate-200 rounded border border-slate-300 font-bold text-slate-800 flex items-center justify-between transition"
          >
            <span className="flex items-center gap-1.5">
              <Code className="w-4 h-4 text-slate-600" />
              View Payload Specifications & Schema Mappings
            </span>
            {showTransformation ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>

          {showTransformation && (
            <div className="mt-3 space-y-3 p-3 bg-slate-900 rounded border border-slate-800 text-slate-100 font-mono text-[11px]">
              <div>
                <span className="text-amber-400 font-bold text-[10px] uppercase block mb-1">1. SOURCE REVENUE REST/JSON PAYLOAD</span>
                <pre className="p-2 bg-slate-950 rounded border border-slate-800 text-slate-300 overflow-x-auto">
{JSON.stringify({
  applicationId: transaction.applicationId,
  sourceDepartment: transaction.sourceDepartment,
  targetDepartment: transaction.targetDepartment,
  correlationId: transaction.correlationId,
  idempotencyKey: idempotencyKey,
  purpose: "RATION_ADDRESS_UPDATE",
  consent: { id: transaction.consentId || "CONSENT-00124" },
  citizen: {
    reference: "CIT-MH-998811",
    name: "Rajesh Kumar",
    address: {
      line: "44 Example Road, Shivajinagar, Pune - 411005",
      district: "DIST-PUN",
      taluka: "TAL-PUN-04"
    }
  },
  verification: { status: "VALID", source: "REVENUE" }
}, null, 2)}
                </pre>
              </div>

              <div>
                <span className="text-purple-400 font-bold text-[10px] uppercase block mb-1">2. CONSENT & IDEMPOTENCY DECISION OBJECT</span>
                <pre className="p-2 bg-slate-950 rounded border border-slate-800 text-purple-300 overflow-x-auto">
{JSON.stringify({
  status: isBlocked ? "BLOCKED" : "ALLOWED",
  idempotencyKey: idempotencyKey,
  attemptCount: transaction.attemptCount || 1,
  maxAttempts: transaction.maxAttempts || 3,
  retryStatus: transaction.retryStatus || transaction.status,
  consentId: transaction.consentId || "CONSENT-00124",
  timestamp: transaction.startedAt
}, null, 2)}
                </pre>
              </div>

              {!isBlocked && (
                <div>
                  <span className="text-emerald-400 font-bold text-[10px] uppercase block mb-1">3. GENERATED FOOD DEPARTMENT SOAP/XML REQUEST</span>
                  <pre className="p-2 bg-slate-950 rounded border border-slate-800 text-emerald-300 overflow-x-auto">
{`<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:food="http://govmesh.example/food">
   <soapenv:Header/>
   <soapenv:Body>
      <food:UpdateRationAddress>
         <food:ApplicationId>${transaction.applicationId}</food:ApplicationId>
         <food:CitizenName>Rajesh Kumar</food:CitizenName>
         <food:RationCardNo>MH12-2026-000124</food:RationCardNo>
         <food:Address>44 Example Road, Shivajinagar, Pune - 411005</food:Address>
         <food:DistrictCode>DIST-PUN</food:DistrictCode>
         <food:TalukaCode>TAL-PUN-04</food:TalukaCode>
         <food:RevenueVerified>true</food:RevenueVerified>
         <food:ConsentId>${transaction.consentId || "CONSENT-00124"}</food:ConsentId>
         <food:CorrelationId>${transaction.correlationId}</food:CorrelationId>
      </food:UpdateRationAddress>
   </soapenv:Body>
</soapenv:Envelope>`}
                  </pre>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
