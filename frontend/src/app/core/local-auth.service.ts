import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MockRole, MockSession } from './models';

interface StoredIdentity {
  username: string;
  roles: MockRole[];
}

const STORAGE_KEY = 'arus-local-identity';

@Injectable({ providedIn: 'root' })
export class LocalAuthService {
  readonly session = signal<MockSession | null>(null);
  readonly identity = signal<StoredIdentity | null>(null);

  constructor(private readonly http: HttpClient) {}

  async login(username: string, roles: MockRole[]): Promise<MockSession> {
    const identity = { username: username.trim(), roles: [...roles] };
    if (!identity.username || !identity.roles.length) {
      throw new Error('Enter a username and select at least one role.');
    }

    this.identity.set(identity);
    this.persist(identity);
    try {
      const session = await firstValueFrom(this.http.get<MockSession>('/api/v1/session'));
      if (!session.mockIdentity) {
        throw new Error('The API did not confirm a local mock identity.');
      }
      this.session.set(session);
      return session;
    } catch (error) {
      this.logout();
      throw error;
    }
  }

  async restore(): Promise<MockSession | null> {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (!stored) return null;

    try {
      const identity = JSON.parse(stored) as StoredIdentity;
      if (!identity.username || !Array.isArray(identity.roles) || !identity.roles.length) {
        this.logout();
        return null;
      }
      this.identity.set(identity);
      const session = await firstValueFrom(this.http.get<MockSession>('/api/v1/session'));
      if (!session.mockIdentity) {
        this.logout();
        return null;
      }
      this.session.set(session);
      return session;
    } catch (error) {
      this.logout();
      throw error;
    }
  }

  logout(): void {
    this.session.set(null);
    this.identity.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  hasAnyRole(...roles: MockRole[]): boolean {
    return roles.some((role) => this.session()?.roles.includes(role));
  }

  private persist(identity: StoredIdentity): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(identity));
  }
}
