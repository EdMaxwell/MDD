import { CommonModule } from '@angular/common';
import { Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { BrandLogoComponent } from '../../shared/ui/brand-logo.component';
import { TopbarComponent } from '../../shared/ui/topbar.component';
import { UiButtonComponent } from '../../shared/ui/ui-button.component';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, BrandLogoComponent, TopbarComponent, UiButtonComponent],
  template: `
    <main class="home-shell">
      <section class="home-page">
        <div class="home-topbar">
          <app-topbar />
        </div>

        <div class="home-panel">
          <app-brand-logo size="medium" />

          @if (authService.currentUser(); as user) {
            <div class="session-box">
              <p class="eyebrow">Accueil</p>
              <h1>Tu es connecte</h1>
              <strong>{{ user.name }}</strong>
              <span>{{ user.email }}</span>
              <app-ui-button variant="outline" (buttonClick)="openProfile()">Voir mon profil</app-ui-button>
              <app-ui-button (buttonClick)="logout()">Se deconnecter</app-ui-button>
            </div>
          }
        </div>
      </section>
    </main>
  `,
  styles: [
    `
      :host {
        display: block;
        min-height: 100dvh;
        font-family: Arial, Helvetica, sans-serif;
        color: #121212;
        background: #ffffff;
      }

      .home-shell {
        min-height: 100dvh;
        display: grid;
        place-items: center;
        padding: 1.25rem;
      }

      .home-page {
        width: min(100%, 1100px);
        min-height: calc(100dvh - 2.5rem);
        display: grid;
        grid-template-rows: 52px 1fr;
      }

      .home-panel {
        display: grid;
        place-items: center;
        align-content: center;
        gap: 2rem;
        padding: 2rem;
      }

      .home-topbar {
        display: block;
      }

      .session-box {
        display: grid;
        gap: 0.45rem;
        justify-items: center;
        text-align: center;
      }

      .eyebrow {
        margin: 0;
        font-size: 12px;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: #666666;
      }

      h1 {
        margin: 0 0 0.35rem;
        font-size: 28px;
        font-weight: 400;
      }

      strong {
        font-size: 22px;
        font-weight: 500;
      }

      span {
        color: #666666;
        font-size: 13px;
      }

      app-ui-button {
        margin-top: 1rem;
      }

      @media (max-width: 768px) {
        .home-shell {
          padding: 0.9rem;
        }

        .home-page {
          width: min(100%, 360px);
          min-height: calc(100dvh - 1.8rem);
          grid-template-rows: 1fr;
        }

        .home-topbar {
          display: none;
        }
      }
    `,
  ],
})
export class HomePageComponent {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  constructor() {
    this.authService.init();

    effect(() => {
      const isCheckingSession = this.authService.checkingSession();
      const isAuthenticated = this.authService.isAuthenticated();

      if (!isAuthenticated && !isCheckingSession) {
        this.router.navigateByUrl('/');
      }
    });
  }

  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/');
  }

  protected openProfile(): void {
    this.router.navigateByUrl('/profile');
  }
}
