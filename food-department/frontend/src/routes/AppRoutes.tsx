import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthLayout } from '../components/layout/AuthLayout';
import { MainLayout } from '../components/layout/MainLayout';
import { ProtectedRoute } from './ProtectedRoute';

import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { RequestsPage } from '../pages/RequestsPage';
import { RationRecordsPage } from '../pages/RationRecordsPage';
import { RationRecordDetailPage } from '../pages/RationRecordDetailPage';
import { ApplicationDetailPage } from '../pages/ApplicationDetailPage';
import { IntegrationMonitorPage } from '../pages/IntegrationMonitorPage';
import { IntegrationTracePage } from '../pages/IntegrationTracePage';
import { FailedAndRetryRequestsPage } from '../pages/FailedAndRetryRequestsPage';
import { ConsentListPage } from '../pages/ConsentListPage';
import { ConsentDetailPage } from '../pages/ConsentDetailPage';
import { AuditLogsPage } from '../pages/AuditLogsPage';
import { NotificationsPage } from '../pages/NotificationsPage';
import { SystemHealthPage } from '../pages/SystemHealthPage';
import { ProfilePage } from '../pages/ProfilePage';
import { NotFoundPage } from '../pages/NotFoundPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Login Route */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      {/* Protected Department Officer Routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/requests" element={<RequestsPage />} />
          <Route path="/ration-records" element={<RationRecordsPage />} />
          <Route path="/ration-records/:id" element={<RationRecordDetailPage />} />
          <Route path="/applications" element={<RequestsPage />} />
          <Route path="/applications/:id" element={<ApplicationDetailPage />} />
          <Route path="/integration" element={<IntegrationMonitorPage />} />
          <Route path="/integration/retries" element={<FailedAndRetryRequestsPage />} />
          <Route path="/integration/:correlationId" element={<IntegrationTracePage />} />
          <Route path="/consent" element={<ConsentListPage />} />
          <Route path="/consent/:consentId" element={<ConsentDetailPage />} />
          <Route path="/audit-logs" element={<AuditLogsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/system-health" element={<SystemHealthPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
};
