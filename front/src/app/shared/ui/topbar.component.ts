import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { BrandLogoComponent } from './brand-logo.component';

/**
 * Shared authenticated navigation bar used by application pages.
 */
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [BrandLogoComponent, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <a routerLink="/home" class="brand-link" aria-label="Retour a l'accueil">
        <app-brand-logo size="small" />
      </a>

      <nav class="topbar-nav" aria-label="Navigation principale">
        @if (authService.isAuthenticated()) {
          <button type="button" class="logout-action" (click)="logout()">Se deconnecter</button>
        }
        <a routerLink="/home" routerLinkActive="is-active" [routerLinkActiveOptions]="{ exact: true }">Articles</a>
        <a routerLink="/topics" routerLinkActive="is-active">Themes</a>
      </nav>

      <a routerLink="/profile" routerLinkActive="is-active-icon" class="profile-link" aria-label="Profil utilisateur">
        <span class="profile-icon">
          <span class="profile-head"></span>
          <span class="profile-body"></span>
        </span>
      </a>
    </header>
  `,
  styles: [
    `
      .topbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 1.75rem;
        min-height: 76px;
        gap: 1rem;
      }

      .brand-link {
        display: inline-flex;
      }

      .topbar-nav {
        display: flex;
        align-items: center;
        gap: 2rem;
        margin-left: auto;
      }

      .topbar-nav a,
      .topbar-nav span,
      .logout-action {
        color: #121212;
        text-decoration: none;
        font-size: 1rem;
      }

      .topbar-nav a.is-active {
        color: #6f57d2;
      }

      .logout-action {
        border: 0;
        background: transparent;
        color: #9b1111;
        font-weight: 700;
        cursor: pointer;
      }

      .profile-link {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        color: inherit;
        text-decoration: none;
      }

      .profile-icon {
        width: 46px;
        height: 46px;
        border: 3px solid #6f57d2;
        border-radius: 999px;
        position: relative;
        display: inline-block;
      }

      .profile-head {
        position: absolute;
        top: 8px;
        left: 50%;
        width: 10px;
        height: 10px;
        border: 3px solid #6f57d2;
        border-radius: 999px;
        transform: translateX(-50%);
      }

      .profile-body {
        position: absolute;
        left: 50%;
        bottom: 8px;
        width: 20px;
        height: 11px;
        border: 3px solid #6f57d2;
        border-top-left-radius: 14px;
        border-top-right-radius: 14px;
        border-bottom: 0;
        transform: translateX(-50%);
      }

      .profile-link.is-active-icon .profile-icon,
      .profile-link.is-active-icon .profile-head,
      .profile-link.is-active-icon .profile-body {
        border-color: #6f57d2;
      }

      @media (max-width: 768px) {
        .topbar {
          min-height: 64px;
          padding: 0 1rem;
        }

        .topbar-nav,
        .profile-link {
          display: none;
        }

        .profile-icon {
          width: 40px;
          height: 40px;
        }
      }
    `,
  ],
})
export class TopbarComponent {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  /**
   * Ends the current session from the navigation action.
   */
  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/');
  }
}
