import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { map } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { BrandLogoComponent } from '../../shared/ui/brand-logo.component';
import { TopbarComponent } from '../../shared/ui/topbar.component';
import { UiButtonComponent } from '../../shared/ui/ui-button.component';

type AuthScreen = 'landing' | 'login' | 'register';

/**
 * Handles the landing, login and registration screens from route data.
 */
@Component({
  selector: 'app-auth-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, BrandLogoComponent, TopbarComponent, UiButtonComponent],
  templateUrl: './auth-page.component.html',
  styleUrl: './auth-page.component.scss',
})
export class AuthPageComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly screen = toSignal(
    this.route.data.pipe(map((data) => (data['screen'] as AuthScreen | undefined) ?? 'landing')),
    { initialValue: 'landing' as AuthScreen },
  );
  protected readonly isLanding = computed(() => this.screen() === 'landing');
  protected readonly isLogin = computed(() => this.screen() === 'login');
  protected readonly isRegister = computed(() => this.screen() === 'register');
  protected readonly title = computed(() =>
    this.isLogin() ? 'Se connecter' : this.isRegister() ? 'Inscription' : 'Bienvenue',
  );

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  constructor() {
    this.authService.init();

    effect(() => {
      this.screen();
      this.prepareFormForCurrentScreen();
    });

    effect(() => {
      const screen = this.screen();
      const isAuthenticated = this.authService.isAuthenticated();
      const isCheckingSession = this.authService.checkingSession();

      if (isAuthenticated && screen !== 'landing') {
        this.router.navigateByUrl('/home');
        return;
      }

      if (isAuthenticated && screen === 'landing') {
        this.router.navigateByUrl('/home');
      }

      if (!isAuthenticated && screen === 'landing' && isCheckingSession) {
        this.errorMessage.set('');
      }
    });
  }

  /**
   * Switches to one of the authentication screens by navigating to its route.
   *
   * @param screen target authentication screen
   */
  protected navigateTo(screen: AuthScreen): void {
    this.router.navigateByUrl(`/${screen}`);
  }

  /**
   * Returns to the unauthenticated landing route.
   */
  protected backToHome(): void {
    this.router.navigateByUrl('/');
  }

  /**
   * Reconfigures form validators for the active screen.
   *
   * <p>The login form accepts either username or email in the email field, while registration
   * requires a valid email and a display name.</p>
   */
  protected prepareFormForCurrentScreen(): void {
    this.errorMessage.set('');

    const nameControl = this.form.controls.name;
    const emailControl = this.form.controls.email;

    if (this.isRegister()) {
      nameControl.addValidators([Validators.required, Validators.minLength(2), Validators.maxLength(100)]);
      emailControl.setValidators([Validators.required, Validators.email]);
    } else {
      nameControl.removeValidators([Validators.required, Validators.minLength(2), Validators.maxLength(100)]);
      nameControl.setValue('');
      emailControl.setValidators([Validators.required]);
    }

    nameControl.updateValueAndValidity();
    emailControl.updateValueAndValidity();
  }

  /**
   * Submits either login or registration depending on the active route data.
   */
  protected submit(): void {
    this.prepareFormForCurrentScreen();
    this.errorMessage.set('');

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);

    const request = this.isLogin()
      ? this.authService.login({
          email: this.form.controls.email.getRawValue(),
          password: this.form.controls.password.getRawValue(),
        })
      : this.authService.register({
          name: this.form.controls.name.getRawValue(),
          email: this.form.controls.email.getRawValue(),
          password: this.form.controls.password.getRawValue(),
        });

    request.subscribe({
      next: () => {
        this.loading.set(false);
        this.form.reset({ name: '', email: '', password: '' });
        this.form.markAsPristine();
        this.form.markAsUntouched();
        this.router.navigateByUrl('/home');
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(this.resolveErrorMessage(error));
      },
    });
  }

  /**
   * Logs out from the topbar action and returns to the landing screen.
   */
  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/');
  }

  /**
   * Checks whether a form control should display a validation error.
   */
  protected hasError(controlName: 'name' | 'email' | 'password', errorCode: string): boolean {
    const control = this.form.controls[controlName];
    return control.touched && control.hasError(errorCode);
  }

  /**
   * Maps backend and network errors to user-facing messages.
   */
  private resolveErrorMessage(error: HttpErrorResponse): string {
    if (error.error?.message) {
      return error.error.message;
    }
    if (error.status === 0) {
      return 'Le backend est inaccessible. Verifie que Spring Boot tourne sur le port 8080.';
    }
    return 'Une erreur est survenue. Reessaie dans un instant.';
  }
}
