import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { TopbarComponent } from '../../../../shared/ui/topbar/topbar.component';
import { ArticleComment, ArticleDetail, ArticleFeedService } from '../../services/article-feed.service';

/**
 * Displays an article detail and lets the authenticated user append comments.
 */
@Component({
  selector: 'app-article-detail-page',
  standalone: true,
  imports: [CommonModule, DatePipe, ReactiveFormsModule, TopbarComponent],
  templateUrl: './article-detail-page.component.html',
  styleUrl: './article-detail-page.component.scss',
})
export class ArticleDetailPageComponent {
  private readonly authService = inject(AuthService);
  private readonly articleFeedService = inject(ArticleFeedService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly article = signal<ArticleDetail | null>(null);
  protected readonly comments = signal<ArticleComment[]>([]);
  protected readonly loadingArticle = signal(true);
  protected readonly submittingComment = signal(false);
  protected readonly feedback = signal('');

  protected readonly commentForm = this.formBuilder.nonNullable.group({
    content: ['', Validators.required],
  });

  constructor() {
    this.authService.init();
    this.loadArticle();

    effect(() => {
      const isCheckingSession = this.authService.checkingSession();
      const isAuthenticated = this.authService.isAuthenticated();

      if (!isAuthenticated && !isCheckingSession) {
        this.router.navigateByUrl('/');
      }
    });
  }

  /**
   * Returns to the feed page.
   */
  protected goBack(): void {
    this.router.navigateByUrl('/home');
  }

  /**
   * Adds a comment to the loaded article and updates the local comment list.
   */
  protected submitComment(): void {
    const article = this.article();
    if (!article || this.commentForm.invalid || this.submittingComment()) {
      this.commentForm.markAllAsTouched();
      return;
    }

    this.submittingComment.set(true);
    this.feedback.set('');

    this.articleFeedService.addComment(article.id, this.commentForm.getRawValue()).subscribe({
      next: (comment) => {
        this.comments.update((comments) => [...comments, comment]);
        this.commentForm.reset();
        this.submittingComment.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.submittingComment.set(false);
        this.feedback.set(this.resolveError(error));
      },
    });
  }

  /**
   * Loads the article referenced by the current route parameter.
   */
  private loadArticle(): void {
    const articleId = this.route.snapshot.paramMap.get('id');
    if (!articleId) {
      this.router.navigateByUrl('/home');
      return;
    }

    this.loadingArticle.set(true);
    this.feedback.set('');

    this.articleFeedService.loadArticle(articleId).subscribe({
      next: (article) => {
        this.article.set(article);
        this.comments.set(article.comments);
        this.loadingArticle.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingArticle.set(false);
        this.feedback.set(this.resolveError(error));
      },
    });
  }

  /**
   * Maps API failures to messages specific to article detail and comments.
   */
  private resolveError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Le backend est inaccessible. Verifie que Spring Boot tourne sur le port 8080.';
    }

    if (error.status === 401 || error.status === 403) {
      return 'La session a expire. Reconnecte-toi pour consulter cet article.';
    }

    if (error.status === 404) {
      return "Cet article n'existe plus.";
    }

    return "Impossible de charger l'article pour le moment.";
  }
}
