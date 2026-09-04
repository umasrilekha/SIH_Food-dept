import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { govmeshService } from '../services/govmeshService';
import { IntegrationTransaction } from '../types/integration';
import { Breadcrumb } from '../components/common/Breadcrumb';
import { StatusBadge } from '../components/common/StatusBadge';
import { LoadingState } from '../components/common/LoadingState';
import { ErrorState } from '../components/common/ErrorState';
import { EmptyState } from '../components/common/EmptyState';
import { formatDate } from '../utils/formatters';
import { RefreshCw, Eye, AlertOctagon, Clock, ShieldAlert } from 'lucide-react';

export const FailedAndRetryRequestsPage: React.FC = () => {
  const [transactions, setTransactions] = useState<IntegrationTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchRetryTransactions = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await govmeshService.getRetryTransactions();
      setTransactions(data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to fetch failed & retrying integration requests.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRetryTransactions();
    // Auto-refresh every 6 seconds to show active retries in real-time
    const interval = setInterval(fetchRetryTransactions, 6000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="space-y-4 text-xs">
      <Breadcrumb items={[{ label: 'System' }, { label: 'Interoperability Monitor', to: '/integration' }, { label: 'Failed & Retry Requests' }]} />

      {/* Header Bar */}
      <div className="bg-white p-3.5 rounded border border-slate-300 shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-base font-extrabold text-blue-950 flex items-center gap-1.5">
              <AlertOctagon className="w-5 h-5 text-amber-600" />
              GovMesh Reliability & Retry Queue
            </h1>
            <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-100 text-amber-900 border border-amber-300 font-mono">
              AUTOMATED RETRY ENGINE (MAX 3 ATTEMPTS)
            </span>
          </div>
          <p className="text-xs text-slate-600 mt-0.5">
            Monitor queued, actively retrying, and final failure integration transactions across digital platforms.
          </p>
        </div>

        <button
          onClick={fetchRetryTransactions}
          className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 border border-slate-300 rounded shadow-xs transition shrink-0"
        >
          <RefreshCw className="w-3.5 h-3.5 mr-1.5 text-slate-500" />
          Refresh Queue
        </button>
      </div>

      {/* Status Summary Banner */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="bg-amber-50/60 border border-amber-300 rounded p-3 flex items-center justify-between">
          <div>
            <span className="text-[10px] font-bold text-amber-900 uppercase font-mono block">ACTIVELY RETRYING</span>
            <span className="text-lg font-extrabold text-amber-950 font-mono">
              {transactions.filter(t => t.retryStatus === 'RETRYING' || t.status === 'RETRYING').length}
            </span>
          </div>
          <Clock className="w-6 h-6 text-amber-600" />
        </div>

        <div className="bg-red-50/60 border border-red-300 rounded p-3 flex items-center justify-between">
          <div>
            <span className="text-[10px] font-bold text-red-900 uppercase font-mono block">FINAL FAILURES (MAX RETRIES)</span>
            <span className="text-lg font-extrabold text-red-950 font-mono">
              {transactions.filter(t => t.retryStatus === 'FINAL_FAILURE').length}
            </span>
          </div>
          <AlertOctagon className="w-6 h-6 text-red-600" />
        </div>

        <div className="bg-slate-100 border border-slate-300 rounded p-3 flex items-center justify-between">
          <div>
            <span className="text-[10px] font-bold text-slate-800 uppercase font-mono block">GATEKEEPER BLOCKED</span>
            <span className="text-lg font-extrabold text-slate-900 font-mono">
              {transactions.filter(t => t.status === 'BLOCKED' || t.retryStatus === 'BLOCKED').length}
            </span>
          </div>
          <ShieldAlert className="w-6 h-6 text-slate-600" />
        </div>
      </div>

      {/* Table */}
      {loading && transactions.length === 0 ? (
        <LoadingState message="Fetching integration retry queue..." />
      ) : error ? (
        <ErrorState message={error} onRetry={fetchRetryTransactions} />
      ) : transactions.length === 0 ? (
        <EmptyState
          title="No Failed or Retrying Requests"
          description="All inter-departmental integration requests are currently operating smoothly with zero active retries."
        />
      ) : (
        <div className="bg-white rounded border border-slate-300 shadow-xs overflow-hidden space-y-2">
          <div className="p-3 border-b border-slate-200 flex justify-between items-center bg-slate-50">
            <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wide">
              FAILED & RETRY TRANSACTION QUEUE
            </h2>
            <span className="text-[10px] text-slate-500 font-mono">Records: {transactions.length}</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-slate-900 text-slate-200 uppercase font-bold tracking-wider border-b border-slate-800 text-[10px]">
                  <th className="py-2.5 px-3 border-r border-slate-800">Application ID</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Correlation ID</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Status</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Attempt</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Failure Reason</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Next Retry</th>
                  <th className="py-2.5 px-3 border-r border-slate-800">Last Attempt</th>
                  <th className="py-2.5 px-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-slate-50 transition">
                    <td className="py-2 px-3 font-mono font-bold text-slate-900">{tx.applicationId}</td>
                    <td className="py-2 px-3 font-mono font-bold text-blue-950">{tx.correlationId}</td>
                    <td className="py-2 px-3">
                      <StatusBadge status={tx.retryStatus || tx.status} />
                    </td>
                    <td className="py-2 px-3 font-mono font-bold text-slate-800">
                      {tx.attemptCount || 1} / {tx.maxAttempts || 3}
                    </td>
                    <td className="py-2 px-3 max-w-xs truncate text-red-900 font-semibold" title={tx.failureMessage || tx.errorMessage || 'N/A'}>
                      {tx.failureCode || tx.errorCode ? (
                        <span className="font-mono text-[10px] px-1.5 py-0.5 bg-red-50 border border-red-200 rounded mr-1">
                          {tx.failureCode || tx.errorCode}
                        </span>
                      ) : null}
                      {tx.failureMessage || tx.errorMessage || 'N/A'}
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
                    <td className="py-2 px-3 text-slate-600 font-medium">{formatDate(tx.lastAttemptAt || tx.startedAt)}</td>
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
