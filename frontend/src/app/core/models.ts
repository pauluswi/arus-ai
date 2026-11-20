export const MOCK_ROLES = [
  'PAYMENT_OPERATOR',
  'RECONCILIATION_OFFICER',
  'OPERATIONS_MANAGER',
  'PAYMENT_ADMIN',
  'COMPLIANCE_OFFICER',
  'AI_GOVERNANCE_OFFICER',
  'SYSTEM_ADMIN',
] as const;

export type MockRole = (typeof MOCK_ROLES)[number];

export interface MockSession {
  username: string;
  roles: MockRole[];
  mockIdentity: boolean;
}

export interface Payment {
  id: string;
  source: 'INTERNAL' | 'EXTERNAL';
  transactionReference: string;
  endToEndId: string | null;
  amount: number;
  currency: string;
  counterparty: string;
  valueDate: string;
  status: string;
  createdAt: string;
}

export interface ReconciliationBatch {
  id: string;
  status: string;
  startedAt: string;
  completedAt: string | null;
  initiatedBy: string;
  resultCount: number;
  matchedCount: number;
  discrepancyCount: number;
  unmatchedCount: number;
}

export interface ReconciliationResult {
  id: string;
  batchId: string;
  internalRecordId: string | null;
  externalRecordId: string | null;
  status: string;
  matchingEvidence: string;
  createdAt: string;
}

export interface PaymentException {
  id: string;
  reconciliationResultId: string;
  category: string;
  severity: string;
  priority: number;
  status: string;
  assignedTo: string | null;
  createdAt: string;
}

export interface InvestigationNote {
  id: string;
  exceptionId: string;
  author: string;
  note: string;
  createdAt: string;
}

export interface InvestigationReport {
  exceptionId: string;
  category: string;
  provider: string;
  fixtureId: string;
  summary: string;
  confidence: { score: number; label: string };
  recommendations: {
    action: string;
    rationale: string;
    confidence: { score: number; label: string };
    evidenceReferences: string[];
  }[];
  evidenceReferences: { type: string; id: string; description: string }[];
  disclaimer: string;
}

export interface ResolutionRequest {
  id: string;
  exceptionId: string;
  action: 'RESOLVE' | 'CLOSE';
  requestedBy: string;
  justification: string;
  status: string;
  requestedAt: string;
  decidedAt: string | null;
}

export interface AuditEntry {
  id: string;
  actor: string;
  action: string;
  aggregateType: string;
  aggregateId: string;
  details: string;
  occurredAt: string;
}
