import { Injectable, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, defer, finalize, map, shareReplay, switchMap, tap, throwError } from 'rxjs';
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
  private refreshInFlight$: Observable<AuthResponse> | null = null;

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
    this.logDebug('init:start');
    if (!isPlatformBrowser(this.platformId)) {
      this.logDebug('init:ssr-skip');
      return;
    }

    this.checkingSession.set(true);
    const token = localStorage.getItem(this.storageKey);
    this.logDebug('init:token-state', { hasToken: !!token });
    const sessionRequest = token
      ? this.fetchCurrentUser().pipe(
          catchError((error) =>
            this.isSessionExpiredError(error)
              ? (this.logWarn('init:/auth/me-expired', this.describeHttpError(error)),
                this.refreshSession().pipe(map((response) => response.user)))
              : throwError(() => error),
          ),
        )
      : this.refreshSession().pipe(map((response) => response.user));

    sessionRequest.subscribe({
      next: (user) => {
        this.logDebug('init:session-restored', this.describeUser(user));
        this.currentUser.set(user);
        this.checkingSession.set(false);
      },
      error: (error) => {
        this.logWarn('init:session-failed', this.describeHttpError(error));
        this.clearSession();
      },
    });
  }

  /**
   * Opens a session with email/password credentials.
   *
   * @param payload login form data
   * @return authentication response observable
   */
  login(payload: LoginPayload): Observable<AuthResponse> {
    this.logDebug('login:start', { email: this.maskLoginIdentifier(payload.email) });
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, payload, { withCredentials: true })
      .pipe(
        tap((response) => {
          this.logDebug('login:success', this.describeAuthResponse(response));
          this.storeSession(response);
        }),
      );
  }

  /**
   * Creates an account and stores the resulting session.
   *
   * @param payload registration form data
   * @return authentication response observable
   */
  register(payload: RegisterPayload): Observable<AuthResponse> {
    this.logDebug('register:start', { email: this.maskLoginIdentifier(payload.email), nameLength: payload.name.length });
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload, { withCredentials: true })
      .pipe(
        tap((response) => {
          this.logDebug('register:success', this.describeAuthResponse(response));
          this.storeSession(response);
        }),
      );
  }

  /**
   * Revokes the refresh-token cookie server-side and clears local client state immediately.
   */
  logout(): void {
    this.http.post<void>(`${environment.apiUrl}/auth/logout`, {}, { withCredentials: true }).subscribe({
      next: () => this.logDebug('logout:success'),
      error: (error) => this.logWarn('logout:request-failed', this.describeHttpError(error)),
    });
    this.logDebug('logout:start');
    this.clearSession();
  }

  /**
   * Loads the full user profile.
   *
   * <p>The fallback keeps older backend states usable when `/users/me` is unavailable but
   * `/auth/me` still works. The source flag lets the profile page display degraded data safely.</p>
   */
  loadProfile(): Observable<LoadedUserProfile> {
    this.logDebug('profile:load:start');
    return this.requestWithRefresh(() => this.http.get<UserProfile>(`${environment.apiUrl}/users/me`, this.authOptions())).pipe(
      map(
        (profile) =>
          ({
            ...profile,
            subscriptionsSource: 'api',
          }) satisfies LoadedUserProfile,
      ),
      tap((profile) => {
        this.logDebug('profile:load:success', { subscriptions: profile.subscriptions.length, source: profile.subscriptionsSource });
        this.currentUser.set(this.toAuthUser(profile));
      }),
      catchError((error) => {
        if (error.status !== 404) {
          return throwError(() => error);
        }

        this.logWarn('profile:load:fallback-to-auth-me', this.describeHttpError(error));
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
    this.logDebug('profile:update:start', { nameLength: payload.name.length, email: this.maskLoginIdentifier(payload.email), hasPassword: !!payload.password });
    return this.requestWithRefresh(() =>
      this.http.put<UserProfile>(`${environment.apiUrl}/users/me`, payload, this.authOptions()),
    ).pipe(
      tap((profile) => {
        this.logDebug('profile:update:success');
        this.currentUser.set(this.toAuthUser(profile));
      }),
    );
  }

  /**
   * Executes an authenticated GET request with automatic refresh on 401.
   */
  authenticatedGet<T>(url: string): Observable<T> {
    this.logDebug('request:get:start', { url: this.shortUrl(url) });
    return this.requestWithRefresh(() => this.http.get<T>(url, this.authOptions()));
  }

  /**
   * Executes an authenticated POST request with automatic refresh on 401.
   */
  authenticatedPost<T>(url: string, body: unknown): Observable<T> {
    this.logDebug('request:post:start', { url: this.shortUrl(url) });
    return this.requestWithRefresh(() => this.http.post<T>(url, body, this.authOptions()));
  }

  /**
   * Executes an authenticated DELETE request with automatic refresh on 401.
   */
  authenticatedDelete<T>(url: string): Observable<T> {
    this.logDebug('request:delete:start', { url: this.shortUrl(url) });
    return this.requestWithRefresh(() => this.http.delete<T>(url, this.authOptions()));
  }

  /**
   * Stores the access token in browser storage and updates reactive user state.
   */
  private storeSession(response: AuthResponse): void {
    this.logDebug('session:store', { userId: response.user.id, userEmail: this.maskLoginIdentifier(response.user.email) });
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
    this.logWarn('session:clear');
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
    this.logDebug('request:/auth/me');
    return this.http.get<AuthUser>(`${environment.apiUrl}/auth/me`, this.authOptions());
  }

  /**
   * Uses the HttpOnly refresh cookie to obtain a new access token.
   */
  private refreshSession(): Observable<AuthResponse> {
    if (this.refreshInFlight$) {
      this.logDebug('refresh:reuse-in-flight');
      return this.refreshInFlight$;
    }

    this.refreshInFlight$ = defer(() =>
      this.http.post<AuthResponse>(`${environment.apiUrl}/auth/refresh`, {}, { withCredentials: true }),
    ).pipe(
      tap((response) => this.storeSession(response)),
      tap(() => this.logDebug('refresh:success')),
      catchError((error) => {
        this.logWarn('refresh:failure', this.describeHttpError(error));
        return throwError(() => error);
      }),
      finalize(() => {
        this.logDebug('refresh:end');
        this.refreshInFlight$ = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    return this.refreshInFlight$;
  }

  /**
   * Wraps authenticated requests with one refresh attempt when the access token has expired.
   */
  private requestWithRefresh<T>(requestFactory: () => Observable<T>): Observable<T> {
    return requestFactory().pipe(
      catchError((error) => {
        if (!this.isSessionExpiredError(error)) {
          this.logWarn('request:non-session-error', this.describeHttpError(error));
          return throwError(() => error);
        }

        return this.refreshSession().pipe(
          catchError((refreshError) => {
            this.logWarn('request:refresh-failed', this.describeHttpError(refreshError));
            this.clearSession();
            return throwError(() => refreshError);
          }),
          switchMap(() => {
            this.logDebug('request:retry-after-refresh');
            return requestFactory();
          }),
        );
      }),
    );
  }

  /**
   * Detects the HTTP statuses used by the backend when a session has expired or can no longer be restored.
   */
  private isSessionExpiredError(error: unknown): boolean {
    return typeof error === 'object' && error !== null && 'status' in error && (error as { status?: number }).status !== undefined
      ? [401, 403].includes((error as { status: number }).status)
      : false;
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
   * Writes a debug log when auth diagnostics are enabled in the environment.
   */
  private logDebug(message: string, details?: unknown): void {
    if (!environment.authDebugEnabled) {
      return;
    }
    console.debug(`[auth] ${message}`, details ?? '');
  }

  /**
   * Writes a warning log when auth diagnostics are enabled in the environment.
   */
  private logWarn(message: string, details?: unknown): void {
    if (!environment.authDebugEnabled) {
      return;
    }
    console.warn(`[auth] ${message}`, details ?? '');
  }

  /**
   * Reduces sensitive values to a short, readable diagnostic representation.
   */
  private describeAuthResponse(response: AuthResponse): { userId: string; email: string; hasRefreshToken: boolean } {
    return {
      userId: response.user.id,
      email: this.maskLoginIdentifier(response.user.email),
      hasRefreshToken: !!response.refreshToken,
    };
  }

  /**
   * Returns a compact description of the authenticated user for logs.
   */
  private describeUser(user: AuthUser): { userId: string; email: string } {
    return {
      userId: user.id,
      email: this.maskLoginIdentifier(user.email),
    };
  }

  /**
   * Keeps URLs readable in logs without dumping the full query string.
   */
  private shortUrl(url: string): string {
    try {
      const parsed = new URL(url);
      return `${parsed.pathname}${parsed.search ? parsed.search.slice(0, 120) : ''}`;
    } catch {
      return url;
    }
  }

  /**
   * Returns a shortened and partially masked identifier for diagnostics.
   */
  private maskLoginIdentifier(value: string): string {
    const trimmed = value.trim();
    const atIndex = trimmed.indexOf('@');
    if (atIndex > 1) {
      return `${trimmed.slice(0, 2)}***${trimmed.slice(atIndex)}`;
    }
    if (trimmed.length <= 4) {
      return `${trimmed[0] ?? '*'}***`;
    }
    return `${trimmed.slice(0, 2)}***${trimmed.slice(-2)}`;
  }

  /**
   * Extracts a compact HTTP status/message summary for logs.
   */
  private describeHttpError(error: unknown): { status?: number; message?: string } {
    if (typeof error === 'object' && error !== null) {
      const typed = error as { status?: number; error?: { message?: string }; message?: string };
      return {
        status: typed.status,
        message: typed.error?.message ?? typed.message,
      };
    }
    return { message: String(error) };
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
