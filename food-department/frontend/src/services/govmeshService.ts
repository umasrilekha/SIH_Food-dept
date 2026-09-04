import { api } from './api';
import {
  IntegrationTransaction,
  IntegrationAttempt,
  CanonicalAddressUpdateRequest,
  CanonicalAddressUpdateResponse,
} from '../types/integration';

export const govmeshService = {
  getTransactions: async (): Promise<IntegrationTransaction[]> => {
    const response = await api.get<IntegrationTransaction[]>('/govmesh/transactions');
    return response.data;
  },

  getRetryTransactions: async (): Promise<IntegrationTransaction[]> => {
    const response = await api.get<IntegrationTransaction[]>('/govmesh/transactions/retries');
    return response.data;
  },

  getTransactionByCorrelationId: async (correlationId: string): Promise<IntegrationTransaction> => {
    const response = await api.get<IntegrationTransaction>(`/govmesh/transactions/${correlationId}`);
    return response.data;
  },

  getAttempts: async (correlationId: string): Promise<IntegrationAttempt[]> => {
    const response = await api.get<IntegrationAttempt[]>(`/govmesh/transactions/${correlationId}/attempts`);
    return response.data;
  },

  getSimulationMode: async (): Promise<{ mode: string }> => {
    const response = await api.get<{ mode: string }>('/govmesh/simulation-mode');
    return response.data;
  },

  setSimulationMode: async (mode: string): Promise<{ mode: string; message: string }> => {
    const response = await api.post<{ mode: string; message: string }>(`/govmesh/simulation-mode?mode=${mode}`);
    return response.data;
  },

  simulateInteroperability: async (
    payload: CanonicalAddressUpdateRequest
  ): Promise<CanonicalAddressUpdateResponse> => {
    const response = await api.post<CanonicalAddressUpdateResponse>(
      '/govmesh/interoperability/address-update',
      payload
    );
    return response.data;
  },
};
