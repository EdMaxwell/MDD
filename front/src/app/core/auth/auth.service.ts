import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, map, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuthUser {
  id: number;
  name: string;
  email: string;
}

export interface UserSubscription {
  id: number;
  name: string;
  description?: string;
}

export interface UserProfile extends AuthUser {
  subscriptions: UserSubscription[];
}

export interface LoadedUserProfile extends UserProfile {
  subscriptionsSource: 'api' | 'fallback';
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

export interface UpdateProfilePayload {
  name: string;
  email: string;
  password?: string;
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
    this.fetchCurrentUser().subscribe({
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

  loadProfile(): Observable<LoadedUserProfile> {
    return this.http.get<UserProfile>(`${environment.apiUrl}/users/me`, this.authOptions()).pipe(
      map(
        (profile) =>
          ({
            ...profile,
            subscriptionsSource: 'api',
          }) satisfies LoadedUserProfile,
      ),
      tap((profile) => this.currentUser.set(this.toAuthUser(profile))),
      catchError((error) => {
        if (error.status !== 404) {
          return throwError(() => error);
        }

        return this.fetchCurrentUser().pipe(
          map(
            (user) =>
              ({
                ...user,
                subscriptions: [],
                subscriptionsSource: 'fallback' as const,
              }) satisfies LoadedUserProfile,
          ),
        );
      }),
    );
  }

  updateProfile(payload: UpdateProfilePayload): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${environment.apiUrl}/users/me`, payload, this.authOptions()).pipe(
      tap((profile) => this.currentUser.set(this.toAuthUser(profile))),
    );
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

  private fetchCurrentUser(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${environment.apiUrl}/auth/me`, this.authOptions());
  }

  private authOptions() {
    const token = isPlatformBrowser(this.platformId) ? localStorage.getItem(this.storageKey) : null;

    return {
      headers: token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders(),
    };
  }

  private toAuthUser(user: AuthUser): AuthUser {
    return {
      id: user.id,
      name: user.name,
      email: user.email,
    };
  }
}
