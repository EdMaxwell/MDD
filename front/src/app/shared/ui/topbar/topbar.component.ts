import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { MenubarModule } from 'primeng/menubar';
import { AuthService } from '../../../core/auth/auth.service';
import { BrandLogoComponent } from '../brand-logo/brand-logo.component';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [
    AvatarModule,
    BrandLogoComponent,
    MenubarModule,
    RouterLink,
    RouterLinkActive,
  ],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss',
})
export class TopbarComponent {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isMobileMenuOpen = signal(false);

  protected readonly desktopNavigationItems = computed<MenuItem[]>(() => [
    ...(this.authService.isAuthenticated()
      ? [
        {
          label: 'Se déconnecter',
          command: () => this.logout(),
        },
      ]
      : []),
    {
      label: 'Articles',
      routerLink: '/home',
      routerLinkActiveOptions: { exact: true },
    },
    {
      label: 'Thèmes',
      routerLink: '/topics',
    },
  ]);

  protected toggleMobileMenu(): void {
    this.isMobileMenuOpen.update((value) => !value);
  }

  protected closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }

  protected logoutFromMobileMenu(): void {
    this.logout();
    this.closeMobileMenu();
  }

  private logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/');
  }
}
