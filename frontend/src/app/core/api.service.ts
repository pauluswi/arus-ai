import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  AuditEntry,
  InvestigationNote,
  InvestigationReport,
  Payment,
  PaymentException,
  ReconciliationBatch,
  ReconciliationResult,
  ResolutionRequest,
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  payments() {
    return this.http.get<Payment[]>('/api/v1/payments');
  }

  createPayment(payment: Omit<Payment, 'id' | 'createdAt'>) {
    return this.http.post<Payment>('/api/v1/payments', payment);
  }

  exceptions() {
    return this.http.get<PaymentException[]>('/api/v1/exceptions');
  }

  reconciliationBatches() {
    return this.http.get<ReconciliationBatch[]>('/api/v1/reconciliation/batches');
  }

  runReconciliation() {
    return this.http.post<ReconciliationBatch>('/api/v1/reconciliation/batches', {});
  }

  reconciliationResults(batchId: string) {
    return this.http.get<ReconciliationResult[]>(
      `/api/v1/reconciliation/batches/${batchId}/results`,
    );
  }

  exceptionNotes(exceptionId: string) {
    return this.http.get<InvestigationNote[]>(`/api/v1/exceptions/${exceptionId}/notes`);
  }

  addNote(exceptionId: string, note: string) {
    return this.http.post<InvestigationNote>(`/api/v1/exceptions/${exceptionId}/notes`, { note });
  }

  assignException(exceptionId: string, assignedTo: string) {
    return this.http.put<PaymentException>(`/api/v1/exceptions/${exceptionId}/assignment`, {
      assignedTo,
    });
  }

  updateSeverity(exceptionId: string, severity: string) {
    return this.http.put<PaymentException>(`/api/v1/exceptions/${exceptionId}/severity`, {
      severity,
    });
  }

  requestResolution(exceptionId: string, action: 'RESOLVE' | 'CLOSE', justification: string) {
    return this.http.post<ResolutionRequest>(
      `/api/v1/exceptions/${exceptionId}/resolution-requests`,
      { action, justification },
    );
  }

  investigationReport(exceptionId: string) {
    return this.http.get<InvestigationReport>(
      `/api/v1/exceptions/${exceptionId}/investigation-report`,
    );
  }

  auditHistory(exceptionId: string) {
    return this.http.get<AuditEntry[]>(`/api/v1/exceptions/${exceptionId}/audit`);
  }

  pendingApprovals() {
    return this.http.get<ResolutionRequest[]>('/api/v1/approvals/pending');
  }

  decideApproval(requestId: string, decision: 'APPROVED' | 'REJECTED', justification: string) {
    return this.http.post(
      `/api/v1/approvals/exception-resolution-requests/${requestId}/decision`,
      { decision, justification },
    );
  }
}
