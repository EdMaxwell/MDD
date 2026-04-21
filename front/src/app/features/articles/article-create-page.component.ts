import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { TopbarComponent } from '../../shared/ui/topbar.component';
import { UiButtonComponent } from '../../shared/ui/ui-button.component';
import { TopicItem, TopicSubscriptionService } from '../topics/topic-subscription.service';
import { ArticleFeedService } from './article-feed.service';

@Component({
  selector: 'app-article-create-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TopbarComponent, UiButtonComponent],
  templateUrl: './article-create-page.component.html',
  styleUrl: './article-create-page.component.scss',
})
export class ArticleCreatePageComponent {
  private readonly authService = inject(AuthService);
  private readonly articleFeedService = inject(ArticleFeedService);
  private readonly topicSubscriptionService = inject(TopicSubscriptionService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);

  protected readonly topics = signal<TopicItem[]>([]);
  protected readonly loadingTopics = signal(true);
  protected readonly submitting = signal(false);
  protected readonly feedback = signal('');

  protected readonly form = this.formBuilder.nonNullable.group({
    topicId: ['', Validators.required],
    title: ['', [Validators.required, Validators.maxLength(180)]],
    content: ['', Validators.required],
  });

  constructor() {
    this.authService.init();
    this.loadTopics();

    effect(() => {
      const isCheckingSession = this.authService.checkingSession();
      const isAuthenticated = this.authService.isAuthenticated();

      if (!isAuthenticated && !isCheckingSession) {
        this.router.navigateByUrl('/');
      }
    });
  }

  protected goBack(): void {
    this.router.navigateByUrl('/home');
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.feedback.set('');

    this.articleFeedService.createArticle(this.form.getRawValue()).subscribe({
      next: (article) => {
        this.submitting.set(false);
        this.router.navigate(['/articles', article.id]);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.feedback.set(this.resolveError(error));
      },
    });
  }

  private loadTopics(): void {
    this.loadingTopics.set(true);
    this.topicSubscriptionService.loadTopics().subscribe({
      next: (topics) => {
        this.topics.set(topics);
        this.loadingTopics.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingTopics.set(false);
        this.feedback.set(this.resolveError(error));
      },
    });
  }

  private resolveError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Le backend est inaccessible. Verifie que Spring Boot tourne sur le port 8080.';
    }

    if (error.status === 401 || error.status === 403) {
      return 'La session a expire. Reconnecte-toi pour creer un article.';
    }

    if (error.status === 404) {
      return "Le theme selectionne n'existe plus.";
    }

    return "Impossible d'enregistrer l'article pour le moment.";
  }
}
