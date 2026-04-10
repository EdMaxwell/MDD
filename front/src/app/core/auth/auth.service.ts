import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuthUser {
  id: number;
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  user: AuthUser;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload extends LoginPayload {
  name: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly storageKey = 'mdd.token';
  private initialized = false;

  readonly currentUser = signal<AuthUser | null>(null);
  readonly checkingSession = signal(false);
  readonly isAuthenticated = computed(() => !!this.currentUser());

  init(): void {
    if (this.initialized) {
      return;
    }

    this.initialized = true;
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const token = localStorage.getItem(this.storageKey);
    if (!token) {
      return;
    }

    this.checkingSession.set(true);
    this.http
      .get<AuthUser>(`${environment.apiUrl}/auth/me`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      .subscribe({
        next: (user) => {
          this.currentUser.set(user);
          this.checkingSession.set(false);
        },
        error: () => this.clearSession(),
      });
  }

  login(payload: LoginPayload) {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, payload)
      .pipe(tap((response) => this.storeSession(response)));
  }

  register(payload: RegisterPayload) {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload)
      .pipe(tap((response) => this.storeSession(response)));
  }

  logout(): void {
    this.clearSession();
  }

  private storeSession(response: AuthResponse): void {
    this.currentUser.set(response.user);
    this.checkingSession.set(false);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.storageKey, response.token);
    }
  }

  private clearSession(): void {
    this.currentUser.set(null);
    this.checkingSession.set(false);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.storageKey);
    }
  }
}
