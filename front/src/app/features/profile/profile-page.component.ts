import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import {
  AuthService,
  LoadedUserProfile,
  UpdateProfilePayload,
} from '../../core/auth/auth.service';
import { TopbarComponent } from '../../shared/ui/topbar/topbar.component';
import { UiButtonComponent } from '../../shared/ui/ui-button/ui-button.component';
import { TopicSubscriptionService } from '../topics/topic-subscription.service';

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

/**
 * Displays and updates the authenticated user's profile and subscriptions.
 */
@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TopbarComponent, UiButtonComponent],
  templateUrl: './profile-page.component.html',
  styleUrl: './profile-page.component.scss',
})
export class ProfilePageComponent {
  private readonly fb = inject(FormBuilder);
  protected readonly authService = inject(AuthService);
  private readonly topicSubscriptionService = inject(TopicSubscriptionService);
  private readonly router = inject(Router);

  protected readonly loadingProfile = signal(true);
  protected readonly savingProfile = signal(false);
  protected readonly profile = signal<LoadedUserProfile | null>(null);
  protected readonly profileError = signal('');
  protected readonly saveError = signal('');
  protected readonly successMessage = signal('');
  protected readonly updatingSubscriptionIds = signal<Set<string>>(new Set());

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    password: ['', [Validators.pattern(passwordPattern)]],
  });

  constructor() {
    this.authService.init();
    this.loadProfile();

    effect(() => {
      const isCheckingSession = this.authService.checkingSession();
      const isAuthenticated = this.authService.isAuthenticated();

      if (!isAuthenticated && !isCheckingSession) {
        this.router.navigateByUrl('/');
      }
    });
  }

  /**
   * Saves profile changes and keeps an empty password as "do not change password".
   */
  protected submit(): void {
    this.successMessage.set('');
    this.saveError.set('');

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: UpdateProfilePayload = {
      email: this.form.controls.email.getRawValue().trim(),
      name: this.form.controls.name.getRawValue().trim(),
    };
    const password = this.form.controls.password.getRawValue().trim();

    if (password) {
      payload.password = password;
    }

    this.savingProfile.set(true);
    this.authService.updateProfile(payload).subscribe({
      next: (profile) => {
        this.savingProfile.set(false);
        this.profile.set({
          ...profile,
          subscriptionsSource: 'api',
        });
        this.form.patchValue({ password: '' });
        this.form.markAsPristine();
        this.form.markAsUntouched();
        this.successMessage.set('Profil mis a jour avec succes.');
      },
      error: (error: HttpErrorResponse) => {
        this.savingProfile.set(false);
        this.saveError.set(this.resolveUpdateError(error));
      },
    });
  }

  /**
   * Logs out from the profile action and returns to the landing route.
   */
  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/');
  }

  /**
   * Checks whether an unsubscribe request is already running for a topic.
   */
  protected isUnsubscribing(topicId: string): boolean {
    return this.updatingSubscriptionIds().has(topicId);
  }

  /**
   * Removes a topic from the user's subscriptions and updates the profile locally.
   */
  protected unsubscribe(topicId: string): void {
    const currentProfile = this.profile();
    if (!currentProfile || this.isUnsubscribing(topicId)) {
      return;
    }

    this.saveError.set('');
    this.successMessage.set('');
    this.trackSubscriptionUpdate(topicId, true);

    this.topicSubscriptionService.unsubscribe(topicId).subscribe({
      next: () => {
        this.profile.update((profile) => {
          if (!profile) {
            return profile;
          }

          return {
            ...profile,
            subscriptions: profile.subscriptions.filter((subscription) => subscription.id !== topicId),
          };
        });
        this.trackSubscriptionUpdate(topicId, false);
        this.successMessage.set('Abonnement supprime avec succes.');
      },
      error: (error: HttpErrorResponse) => {
        this.trackSubscriptionUpdate(topicId, false);
        this.saveError.set(this.resolveUnsubscribeError(error));
      },
    });
  }

  /**
   * Checks whether a form control should display a validation error.
   */
  protected hasError(controlName: 'email' | 'name' | 'password', errorCode: string): boolean {
    const control = this.form.controls[controlName];
    return control.touched && control.hasError(errorCode);
  }

  /**
   * Loads the profile and hydrates the form with the returned data.
   */
  private loadProfile(): void {
    this.loadingProfile.set(true);
    this.profileError.set('');
    this.successMessage.set('');

    this.authService.loadProfile().subscribe({
      next: (profile) => {
        this.loadingProfile.set(false);
        this.profile.set(profile);
        this.form.reset({
          email: profile.email,
          name: profile.name,
          password: '',
        });
      },
      error: (error: HttpErrorResponse) => {
        this.loadingProfile.set(false);
        this.profileError.set(this.resolveProfileError(error));
      },
    });
  }

  /**
   * Maps profile-loading failures to user-facing messages.
   */
  private resolveProfileError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Le backend est inaccessible. Verifie que Spring Boot tourne sur le port 8080.';
    }

    if (error.status === 403 || error.status === 401) {
      return 'La session a expire. Reconnecte-toi pour consulter ton profil.';
    }

    return 'Impossible de charger le profil pour le moment.';
  }

  /**
   * Maps profile-update failures, including backend validation details, to user-facing messages.
   */
  private resolveUpdateError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Le backend est inaccessible. Verifie que Spring Boot tourne sur le port 8080.';
    }

    if (error.status === 409) {
      return "Cette adresse e-mail est deja utilisee.";
    }

    if (error.status === 400 && error.error?.details) {
      const firstDetail = Object.values(error.error.details)[0];
      if (typeof firstDetail === 'string') {
        return firstDetail;
      }
    }

    if (error.error?.message) {
      return error.error.message;
    }

    return 'Impossible de mettre a jour le profil pour le moment.';
  }

  /**
   * Tracks in-flight subscription updates without mutating the existing signal value.
   */
  private trackSubscriptionUpdate(topicId: string, updating: boolean): void {
    this.updatingSubscriptionIds.update((topicIds) => {
      const nextTopicIds = new Set(topicIds);
      if (updating) {
        nextTopicIds.add(topicId);
      } else {
        nextTopicIds.delete(topicId);
      }
      return nextTopicIds;
    });
  }

  /**
   * Maps unsubscribe failures to user-facing messages.
   */
  private resolveUnsubscribeError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Le backend est inaccessible. Verifie que Spring Boot tourne sur le port 8080.';
    }

    if (error.status === 401 || error.status === 403) {
      return 'La session a expire. Reconnecte-toi pour modifier tes abonnements.';
    }

    if (error.status === 404) {
      return "Ce theme n'existe plus.";
    }

    return "Impossible de supprimer l'abonnement pour le moment.";
  }
}
