import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { PaginatorModule } from 'primeng/paginator';
import { PaginatorState } from 'primeng/types/paginator';
import {
  ArticleFeedItem,
  ArticleFeedService,
  ArticleSortDirection,
} from '../articles/article-feed.service';
import { AuthService } from '../../core/auth/auth.service';
import { ArticleCardComponent } from '../articles/article-card.component';
import { TopbarComponent } from '../../shared/ui/topbar/topbar.component';
import { UiButtonComponent } from '../../shared/ui/ui-button/ui-button.component';

/**
 * Displays the authenticated user's article feed and feed-level actions.
 */
@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, TopbarComponent, UiButtonComponent, ArticleCardComponent, PaginatorModule],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss',
})
export class HomePageComponent {
  protected readonly pageSize = 6;
  protected readonly authService = inject(AuthService);
  private readonly articleFeedService = inject(ArticleFeedService);
  private readonly router = inject(Router);

  protected readonly articles = signal<ArticleFeedItem[]>([]);
  protected readonly loadingFeed = signal(true);
  protected readonly feedError = signal('');
  protected readonly sortDirection = signal<ArticleSortDirection>('desc');
  protected readonly currentPage = signal(0);
  protected readonly totalArticles = signal(0);

  constructor() {
    this.authService.init();
    this.loadFeed();

    effect(() => {
      const isCheckingSession = this.authService.checkingSession();
      const isAuthenticated = this.authService.isAuthenticated();

      if (!isAuthenticated && !isCheckingSession) {
        this.router.navigateByUrl('/');
      }
    });
  }

  /**
   * Toggles feed ordering between newest first and oldest first, then reloads the feed.
   */
  protected toggleSort(): void {
    this.sortDirection.update((direction) => (direction === 'desc' ? 'asc' : 'desc'));
    this.currentPage.set(0);
    this.loadFeed();
  }

  /**
   * Loads the page selected from the feed paginator.
   */
  protected changePage(event: PaginatorState): void {
    this.currentPage.set(event.page ?? 0);
    this.loadFeed();
  }

  /**
   * Opens the article creation page.
   */
  protected createArticle(): void {
    this.router.navigateByUrl('/articles/new');
  }

  /**
   * Opens an article detail page from a selected feed card.
   */
  protected openArticle(articleId: string): void {
    this.router.navigate(['/articles', articleId]);
  }

  /**
   * Loads the feed using the current sort direction.
   */
  private loadFeed(): void {
    this.loadingFeed.set(true);
    this.feedError.set('');

    this.articleFeedService.loadFeed(this.sortDirection(), this.currentPage(), this.pageSize).subscribe({
      next: (page) => {
        this.articles.set(page.content);
        this.currentPage.set(page.page);
        this.totalArticles.set(page.totalElements);
        this.loadingFeed.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingFeed.set(false);
        this.feedError.set(this.resolveFeedError(error));
      },
    });
  }

  /**
   * Maps feed API failures to user-facing messages.
   */
  private resolveFeedError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Le backend est inaccessible. Verifie que Spring Boot tourne sur le port 8080.';
    }

    if (error.status === 401 || error.status === 403) {
      return 'La session a expire. Reconnecte-toi pour consulter les articles.';
    }

    return 'Impossible de charger les articles pour le moment.';
  }
}
