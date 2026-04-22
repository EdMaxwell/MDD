import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, map, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Public user identity stored in the client authentication state. */
export interface AuthUser {
  id: string;
  name: string;
  email: string;
}

/** Topic subscription shown in the profile page. */
export interface UserSubscription {
  id: string;
  name: string;
  description?: string;
}

/** Full profile returned by the profile API. */
export interface UserProfile extends AuthUser {
  subscriptions: UserSubscription[];
}

/** Profile response enriched with the source used to load subscriptions. */
export interface LoadedUserProfile extends UserProfile {
  subscriptionsSource: 'api' | 'fallback';
}

/** Authentication response returned by login, registration and refresh endpoints. */
export interface AuthResponse {
  token: string;
  refreshToken?: string;
  user: AuthUser;
}

/** Login form payload. */
export interface LoginPayload {
  email: string;
  password: string;
}

/** Registration form payload. */
export interface RegisterPayload extends LoginPayload {
  name: string;
}

/** Profile update payload accepted by the backend. */
export interface UpdateProfilePayload {
  name: string;
  email: string;
  password?: string;
}

/**
 * Centralizes browser session state, access-token storage and authenticated HTTP helpers.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly storageKey = 'mdd.token';
  private initialized = false;

  readonly currentUser = signal<AuthUser | null>(null);
  readonly checkingSession = signal(false);
  readonly isAuthenticated = computed(() => !!this.currentUser());

  /**
   * Restores the browser session once per application lifetime.
   *
   * <p>SSR renders do not access localStorage. In the browser, an existing access token is
   * checked first; if it is expired, the HttpOnly refresh cookie is used to obtain a new one.</p>
   */
  init(): void {
    if (this.initialized) {
      return;
    }

    this.initialized = true;
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.checkingSession.set(true);
    const token = localStorage.getItem(this.storageKey);
    const sessionRequest = token
      ? this.fetchCurrentUser().pipe(catchError(() => this.refreshSession().pipe(map((response) => response.user))))
      : this.refreshSession().pipe(map((response) => response.user));

    sessionRequest.subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.checkingSession.set(false);
      },
      error: () => this.clearSession(),
    });
  }

  /**
   * Opens a session with email/password credentials.
   *
   * @param payload login form data
   * @return authentication response observable
   */
  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, payload, { withCredentials: true })
      .pipe(tap((response) => this.storeSession(response)));
  }

  /**
   * Creates an account and stores the resulting session.
   *
   * @param payload registration form data
   * @return authentication response observable
   */
  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload, { withCredentials: true })
      .pipe(tap((response) => this.storeSession(response)));
  }

  /**
   * Revokes the refresh-token cookie server-side and clears local client state immediately.
   */
  logout(): void {
    this.http.post<void>(`${environment.apiUrl}/auth/logout`, {}, { withCredentials: true }).subscribe({
      error: () => undefined,
    });
    this.clearSession();
  }

  /**
   * Loads the full user profile.
   *
   * <p>The fallback keeps older backend states usable when `/users/me` is unavailable but
   * `/auth/me` still works. The source flag lets the profile page display degraded data safely.</p>
   */
  loadProfile(): Observable<LoadedUserProfile> {
    return this.requestWithRefresh(() => this.http.get<UserProfile>(`${environment.apiUrl}/users/me`, this.authOptions())).pipe(
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

        return this.requestWithRefresh(() => this.fetchCurrentUser()).pipe(
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

  /**
   * Updates profile fields and refreshes the lightweight authenticated user signal.
   *
   * @param payload profile update data
   * @return updated profile response
   */
  updateProfile(payload: UpdateProfilePayload): Observable<UserProfile> {
    return this.requestWithRefresh(() =>
      this.http.put<UserProfile>(`${environment.apiUrl}/users/me`, payload, this.authOptions()),
    ).pipe(
      tap((profile) => this.currentUser.set(this.toAuthUser(profile))),
    );
  }

  /**
   * Executes an authenticated GET request with automatic refresh on 401.
   */
  authenticatedGet<T>(url: string): Observable<T> {
    return this.requestWithRefresh(() => this.http.get<T>(url, this.authOptions()));
  }

  /**
   * Executes an authenticated POST request with automatic refresh on 401.
   */
  authenticatedPost<T>(url: string, body: unknown): Observable<T> {
    return this.requestWithRefresh(() => this.http.post<T>(url, body, this.authOptions()));
  }

  /**
   * Executes an authenticated DELETE request with automatic refresh on 401.
   */
  authenticatedDelete<T>(url: string): Observable<T> {
    return this.requestWithRefresh(() => this.http.delete<T>(url, this.authOptions()));
  }

  /**
   * Stores the access token in browser storage and updates reactive user state.
   */
  private storeSession(response: AuthResponse): void {
    this.currentUser.set(response.user);
    this.checkingSession.set(false);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.storageKey, response.token);
    }
  }

  /**
   * Clears all client-side authentication state.
   */
  private clearSession(): void {
    this.currentUser.set(null);
    this.checkingSession.set(false);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.storageKey);
    }
  }

  /**
   * Loads the lightweight authenticated user from the auth API.
   */
  private fetchCurrentUser(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${environment.apiUrl}/auth/me`, this.authOptions());
  }

  /**
   * Uses the HttpOnly refresh cookie to obtain a new access token.
   */
  private refreshSession(): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/refresh`, {}, { withCredentials: true })
      .pipe(tap((response) => this.storeSession(response)));
  }

  /**
   * Wraps authenticated requests with one refresh attempt when the access token has expired.
   */
  private requestWithRefresh<T>(requestFactory: () => Observable<T>): Observable<T> {
    return requestFactory().pipe(
      catchError((error) => {
        if (error.status !== 401) {
          return throwError(() => error);
        }

        return this.refreshSession().pipe(
          catchError((refreshError) => {
            this.clearSession();
            return throwError(() => refreshError);
          }),
          switchMap(() => requestFactory()),
        );
      }),
    );
  }

  /**
   * Builds HTTP options with the current access token and refresh-cookie support.
   */
  private authOptions() {
    const token = isPlatformBrowser(this.platformId) ? localStorage.getItem(this.storageKey) : null;

    return {
      headers: token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders(),
      withCredentials: true,
    };
  }

  /**
   * Drops profile-only fields before storing the current authenticated user.
   */
  private toAuthUser(user: AuthUser): AuthUser {
    return {
      id: user.id,
      name: user.name,
      email: user.email,
    };
  }
}
