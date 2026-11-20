import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './core/api.service';
import { LocalAuthService } from './core/local-auth.service';
import { mockIdentityInterceptor } from './core/mock-identity.interceptor';
import {
  AuditEntry,
  InvestigationNote,
  InvestigationReport,
  MOCK_ROLES,
  MockRole,
  Payment,
  PaymentException,
  ReconciliationBatch,
  ReconciliationResult,
  ResolutionRequest,
} from './core/models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  readonly availableRoles = MOCK_ROLES;
  readonly busy = signal(false);

  page = 'dashboard';
  username = 'demo-operator';
  selectedRoles: MockRole[] = ['PAYMENT_OPERATOR'];
  errorMessage = '';
  successMessage = '';
  payments: Payment[] = [];
  exceptions: PaymentException[] = [];
  batches: ReconciliationBatch[] = [];
  pendingApprovals: ResolutionRequest[] = [];
  selectedExceptionId: string | null = null;
  selectedBatchId: string | null = null;
  reconciliationResults: ReconciliationResult[] = [];
  notes: InvestigationNote[] = [];
  auditEntries: AuditEntry[] = [];
  investigationReport: InvestigationReport | null = null;
  assignedTo = '';
  severity = 'MEDIUM';
  investigationNote = '';
  resolutionAction: 'RESOLVE' | 'CLOSE' = 'RESOLVE';
  resolutionJustification = '';
  approvalJustification = '';
  paymentForm = {
    source: 'INTERNAL',
    transactionReference: '',
    endToEndId: '',
    amount: '',
    currency: 'EUR',
    counterparty: '',
    valueDate: new Date().toISOString().slice(0, 10),
    status: 'RECEIVED',
  };

  constructor(
    readonly auth: LocalAuthService,
    private readonly api: ApiService,
  ) {}

  ngOnInit(): void {
    void this.restoreSession();
  }

  get selectedException(): PaymentException | undefined {
    return this.exceptions.find((item) => item.id === this.selectedExceptionId);
  }

  get canViewExceptions(): boolean {
    return this.auth.hasAnyRole(
      'PAYMENT_OPERATOR',
      'RECONCILIATION_OFFICER',
      'OPERATIONS_MANAGER',
      'COMPLIANCE_OFFICER',
      'PAYMENT_ADMIN',
    );
  }

  get canViewReconciliation(): boolean {
    return this.auth.hasAnyRole(
      'RECONCILIATION_OFFICER',
      'OPERATIONS_MANAGER',
      'COMPLIANCE_OFFICER',
    );
  }

  get canRunReconciliation(): boolean {
    return this.auth.hasAnyRole('RECONCILIATION_OFFICER', 'OPERATIONS_MANAGER');
  }

  get canManageApprovals(): boolean {
    return this.auth.hasAnyRole('OPERATIONS_MANAGER', 'COMPLIANCE_OFFICER');
  }

  get canAssign(): boolean {
    return this.auth.hasAnyRole(
      'OPERATIONS_MANAGER',
      'PAYMENT_ADMIN',
      'RECONCILIATION_OFFICER',
    );
  }

  get canChangeSeverity(): boolean {
    return this.auth.hasAnyRole(
      'OPERATIONS_MANAGER',
      'COMPLIANCE_OFFICER',
      'PAYMENT_ADMIN',
    );
  }

  get canInvestigate(): boolean {
    return this.auth.hasAnyRole(
      'PAYMENT_OPERATOR',
      'RECONCILIATION_OFFICER',
      'OPERATIONS_MANAGER',
    );
  }

  get canViewAudit(): boolean {
    return this.auth.hasAnyRole(
      'RECONCILIATION_OFFICER',
      'OPERATIONS_MANAGER',
      'COMPLIANCE_OFFICER',
      'PAYMENT_ADMIN',
    );
  }

  async login(): Promise<void> {
    this.clearMessages();
    await this.perform(async () => {
      await this.auth.login(this.username, this.selectedRoles);
      await this.loadData();
    }, 'Local demo session started.');
  }

  logout(): void {
    this.auth.logout();
    this.payments = [];
    this.exceptions = [];
    this.batches = [];
    this.pendingApprovals = [];
    this.clearMessages();
  }

  toggleRole(role: MockRole, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedRoles = checked
      ? [...this.selectedRoles, role]
      : this.selectedRoles.filter((item) => item !== role);
  }

  async navigate(page: string): Promise<void> {
    this.page = page;
    this.clearMessages();
    await this.perform(() => this.loadData());
  }

  async refresh(): Promise<void> {
    await this.perform(() => this.loadData(), 'Data refreshed.');
  }

  async createPayment(form: NgForm): Promise<void> {
    if (form.invalid) return;
    await this.perform(async () => {
      await firstValueFrom(
        this.api.createPayment({
          ...this.paymentForm,
          source: this.paymentForm.source as 'INTERNAL' | 'EXTERNAL',
          amount: Number(this.paymentForm.amount),
          endToEndId: this.paymentForm.endToEndId || null,
        }),
      );
      this.paymentForm = {
        source: 'INTERNAL',
        transactionReference: '',
        endToEndId: '',
        amount: '',
        currency: 'EUR',
        counterparty: '',
        valueDate: new Date().toISOString().slice(0, 10),
        status: 'RECEIVED',
      };
      form.resetForm(this.paymentForm);
      await this.loadData();
    }, 'Payment created.');
  }

  async runReconciliation(): Promise<void> {
    await this.perform(async () => {
      const batch = await firstValueFrom(this.api.runReconciliation());
      await this.loadData();
      await this.selectBatch(batch.id);
    }, 'Reconciliation completed.');
  }

  async selectBatch(batchId: string): Promise<void> {
    this.selectedBatchId = batchId;
    this.reconciliationResults = await firstValueFrom(this.api.reconciliationResults(batchId));
  }

  async selectException(exception: PaymentException): Promise<void> {
    this.selectedExceptionId = exception.id;
    this.assignedTo = exception.assignedTo ?? '';
    this.severity = exception.severity;
    this.notes = [];
    this.auditEntries = [];
    this.investigationReport = null;
    await this.perform(async () => {
      if (this.canInvestigate || this.auth.hasAnyRole('COMPLIANCE_OFFICER', 'PAYMENT_ADMIN')) {
        this.notes = await firstValueFrom(this.api.exceptionNotes(exception.id));
      }
    });
  }

  async addInvestigationNote(): Promise<void> {
    if (!this.selectedExceptionId || !this.investigationNote.trim()) return;
    await this.perform(async () => {
      await firstValueFrom(
        this.api.addNote(this.selectedExceptionId!, this.investigationNote.trim()),
      );
      this.investigationNote = '';
      this.notes = await firstValueFrom(this.api.exceptionNotes(this.selectedExceptionId!));
      await this.loadData();
    }, 'Investigation note added.');
  }

  async assignException(): Promise<void> {
    if (!this.selectedExceptionId || !this.assignedTo.trim()) return;
    await this.perform(async () => {
      await firstValueFrom(
        this.api.assignException(this.selectedExceptionId!, this.assignedTo.trim()),
      );
      await this.loadData();
    }, 'Exception assignment updated.');
  }

  async updateSeverity(): Promise<void> {
    if (!this.selectedExceptionId) return;
    await this.perform(async () => {
      await firstValueFrom(
        this.api.updateSeverity(this.selectedExceptionId!, this.severity),
      );
      await this.loadData();
    }, 'Exception severity updated.');
  }

  async requestResolution(): Promise<void> {
    if (!this.selectedExceptionId || !this.resolutionJustification.trim()) return;
    await this.perform(async () => {
      await firstValueFrom(
        this.api.requestResolution(
          this.selectedExceptionId!,
          this.resolutionAction,
          this.resolutionJustification.trim(),
        ),
      );
      this.resolutionJustification = '';
      await this.loadData();
    }, 'Approval request submitted.');
  }

  async loadInvestigationReport(): Promise<void> {
    if (!this.selectedExceptionId) return;
    await this.perform(async () => {
      this.investigationReport = await firstValueFrom(
        this.api.investigationReport(this.selectedExceptionId!),
      );
    }, 'Investigation report loaded.');
  }

  async loadAuditHistory(): Promise<void> {
    if (!this.selectedExceptionId) return;
    await this.perform(async () => {
      this.auditEntries = await firstValueFrom(
        this.api.auditHistory(this.selectedExceptionId!),
      );
    }, 'Audit history loaded.');
  }

  async decideApproval(
    request: ResolutionRequest,
    decision: 'APPROVED' | 'REJECTED',
  ): Promise<void> {
    if (!this.approvalJustification.trim()) {
      this.errorMessage = 'Enter a justification before deciding an approval.';
      return;
    }
    await this.perform(async () => {
      await firstValueFrom(
        this.api.decideApproval(
          request.id,
          decision,
          this.approvalJustification.trim(),
        ),
      );
      this.approvalJustification = '';
      await this.loadData();
    }, `Request ${decision.toLowerCase()}.`);
  }

  formatDate(value: string | null | undefined): string {
    return value ? new Date(value).toLocaleString() : '—';
  }

  get today(): Date {
    return new Date();
  }

  get openExceptionCount(): number {
    return this.exceptions.filter((exception) => !['CLOSED', 'RESOLVED'].includes(exception.status)).length;
  }

  get criticalExceptionCount(): number {
    return this.exceptions.filter((exception) => exception.severity === 'CRITICAL').length;
  }

  statusClass(status: string): string {
    return `status-${status.toLowerCase().replaceAll('_', '-')}`;
  }

  severityClass(severity: string): string {
    return `severity-${severity.toLowerCase()}`;
  }

  trackById(_index: number, item: { id: string }): string {
    return item.id;
  }

  private async restoreSession(): Promise<void> {
    try {
      const session = await this.auth.restore();
      if (session) {
        this.username = session.username;
        this.selectedRoles = [...session.roles];
        await this.loadData();
      }
    } catch (error) {
      this.errorMessage = this.errorText(error);
    }
  }

  private async loadData(): Promise<void> {
    this.payments = await firstValueFrom(this.api.payments());
    this.exceptions = this.canViewExceptions
      ? await firstValueFrom(this.api.exceptions())
      : [];
    this.batches = this.canViewReconciliation
      ? await firstValueFrom(this.api.reconciliationBatches())
      : [];
    this.pendingApprovals = this.canManageApprovals
      ? await firstValueFrom(this.api.pendingApprovals())
      : [];
    if (
      this.selectedExceptionId &&
      !this.exceptions.some((exception) => exception.id === this.selectedExceptionId)
    ) {
      this.selectedExceptionId = null;
    }
    if (this.selectedBatchId && !this.batches.some((batch) => batch.id === this.selectedBatchId)) {
      this.selectedBatchId = null;
      this.reconciliationResults = [];
    }
  }

  private async perform(action: () => Promise<unknown>, success?: string): Promise<void> {
    this.busy.set(true);
    this.errorMessage = '';
    this.successMessage = '';
    try {
      await action();
      if (success) this.successMessage = success;
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy.set(false);
    }
  }

  private errorText(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const details = error.error?.detail ?? error.error?.message;
      return typeof details === 'string'
        ? details
        : `${error.status ? `${error.status}: ` : ''}${error.message}`;
    }
    return error instanceof Error ? error.message : 'The request could not be completed.';
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
