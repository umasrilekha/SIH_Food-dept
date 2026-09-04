export interface IntegrationTransaction {
  id: number;
  applicationId: string;
  correlationId: string;
  sourceDepartment: string;
  targetDepartment: string;
  operation: string;
  sourceProtocol: string;
  targetProtocol: string;
  status: 'RECEIVED' | 'TRANSFORMING' | 'SENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | 'BLOCKED' | 'RETRYING' | 'FINAL_FAILURE' | string;
  consentStatus?: 'ALLOWED' | 'BLOCKED' | string;
  consentFailureReason?: string;
  consentId?: string;
  startedAt: string;
  completedAt?: string;
  errorCode?: string;
  errorMessage?: string;
  rawSourceJson?: string;
  rawCanonicalJson?: string;
  rawSoapRequestXml?: string;
  rawSoapResponseXml?: string;

  // Phase 6 Reliability & Retry Fields
  idempotencyKey?: string;
  attemptCount?: number;
  maxAttempts?: number;
  lastAttemptAt?: string;
  nextRetryAt?: string;
  failureCode?: string;
  failureMessage?: string;
  retryStatus?: string;
  isDuplicate?: boolean;
}

export interface IntegrationAttempt {
  id: number;
  correlationId: string;
  applicationId: string;
  idempotencyKey?: string;
  attemptNumber: number;
  protocol: string;
  status: 'SUCCESS' | 'FAILED' | 'TIMEOUT' | 'SERVICE_UNAVAILABLE' | 'BLOCKED' | string;
  failureCode?: string;
  failureMessage?: string;
  attemptAt: string;
}

export interface CanonicalAddressUpdateRequest {
  applicationId: string;
  sourceDepartment: string;
  targetDepartment: string;
  correlationId: string;
  idempotencyKey?: string;
  purpose?: string;
  requestedFields?: string[];
  citizen: {
    reference: string;
    name: string;
    address: {
      line: string;
      district: string;
      taluka: string;
    };
  };
  verification: {
    status: string;
    source: string;
  };
  consent: {
    id: string;
  };
}

export interface CanonicalAddressUpdateResponse {
  applicationId: string;
  status: 'SUCCESS' | 'FAILED' | 'BLOCKED' | 'RETRYING' | 'PROCESSING' | string;
  message: string;
  correlationId: string;
  targetDepartment: string;
  errorCode?: string;
}
