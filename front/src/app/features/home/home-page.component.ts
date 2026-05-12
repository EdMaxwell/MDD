import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PaginatorState } from 'primeng/types/paginator';
import {
  ARTICLE_FEED_PAGE_SIZE_OPTIONS,
  ArticleFeedItem,
  ArticleFeedService,
  ArticleSortDirection,
  DEFAULT_ARTICLE_FEED_PAGE_SIZE,
} from '../articles/services/article-feed.service';
import { AuthService } from '../../core/auth/auth.service';
import { ArticleCardComponent } from '../articles/components/article-card/article-card.component';
import { TopbarComponent } from '../../shared/ui/topbar/topbar.component';
import { UiButtonComponent } from '../../shared/ui/ui-button/ui-button.component';
import { PaginatedCardGridComponent } from '../../shared/ui/paginated-card-grid/paginated-card-grid.component';

/**
 * Displays the authenticated user's article feed and feed-level actions.
 */
@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, TopbarComponent, UiButtonComponent, ArticleCardComponent, PaginatedCardGridComponent],
  templateUrl: './home-page.component.html',
  styleUrl: './home-page.component.scss',
})
export class HomePageComponent {
  protected readonly pageSizeOptions = [...ARTICLE_FEED_PAGE_SIZE_OPTIONS];
  protected readonly authService = inject(AuthService);
  private readonly articleFeedService = inject(ArticleFeedService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly articles = signal<ArticleFeedItem[]>([]);
  protected readonly loadingFeed = signal(true);
  protected readonly feedError = signal('');
  protected readonly sortDirection = signal<ArticleSortDirection>('desc');
  protected readonly currentPage = signal(0);
  protected readonly pageSize = signal(DEFAULT_ARTICLE_FEED_PAGE_SIZE);
  protected readonly totalArticles = signal(0);

  constructor() {
    this.authService.init();
    this.restoreFeedStateFromQueryParams();
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
    this.pageSize.set(event.rows ?? DEFAULT_ARTICLE_FEED_PAGE_SIZE);
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
    this.router.navigate(['/articles', articleId], {
      queryParams: {
        page: this.currentPage(),
        size: this.pageSize(),
        sort: this.sortDirection(),
      },
    });
  }

  /**
   * Restores the feed state from URL query parameters when returning from article detail.
   */
  private restoreFeedStateFromQueryParams(): void {
    const queryParams = this.route.snapshot.queryParamMap;
    const page = this.parsePositiveInteger(queryParams.get('page'));
    const size = this.parsePositiveInteger(queryParams.get('size'));
    const sort = queryParams.get('sort');

    if (page !== null) {
      this.currentPage.set(page);
    }

    if (size !== null && this.pageSizeOptions.some((option) => option === size)) {
      this.pageSize.set(size);
    }

    if (sort === 'asc' || sort === 'desc') {
      this.sortDirection.set(sort);
    }
  }

  /**
   * Loads the feed using the current sort direction.
   */
  private loadFeed(): void {
    this.loadingFeed.set(true);
    this.feedError.set('');

    this.articleFeedService.loadFeed(this.sortDirection(), this.currentPage(), this.pageSize()).subscribe({
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

  /**
   * Parses a non-negative integer from a query parameter.
   */
  private parsePositiveInteger(value: string | null): number | null {
    if (value === null) {
      return null;
    }

    const parsedValue = Number.parseInt(value, 10);
    return Number.isNaN(parsedValue) || parsedValue < 0 ? null : parsedValue;
  }
}
