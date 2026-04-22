import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

/** Supported creation-date sort directions for the article feed. */
export type ArticleSortDirection = 'desc' | 'asc';

/** Compact article data rendered in the feed. */
export interface ArticleFeedItem {
  id: string;
  title: string;
  content: string;
  createdAt: string;
  authorId: string;
  authorName: string;
  topicId: string;
  topicName: string;
}

/** Generic paginated response returned by the backend. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

type ArticleFeedApiResponse = ArticleFeedItem[] | PageResponse<ArticleFeedItem>;

/** Comment data returned by the article detail API. */
export interface ArticleComment {
  id: string;
  content: string;
  createdAt: string;
  authorId: string;
  authorName: string;
}

/** Full article detail with comments. */
export interface ArticleDetail extends ArticleFeedItem {
  comments: ArticleComment[];
}

/** Payload used to create an article. */
export interface CreateArticlePayload {
  topicId: string;
  title: string;
  content: string;
}

/** Payload used to add a comment. */
export interface CreateCommentPayload {
  content: string;
}

/**
 * Provides authenticated article feed, detail, creation and comment API calls.
 */
@Injectable({ providedIn: 'root' })
export class ArticleFeedService {
  private readonly authService = inject(AuthService);

  /**
   * Loads one page of the current user's feed from followed topics.
   */
  loadFeed(sort: ArticleSortDirection, page = 0, size = 6): Observable<PageResponse<ArticleFeedItem>> {
    const params = new URLSearchParams({
      sort,
      page: String(page),
      size: String(size),
    });
    return this.authService
      .authenticatedGet<ArticleFeedApiResponse>(`${environment.apiUrl}/posts/feed?${params}`)
      .pipe(map((response) => this.normalizeFeedPage(response, page, size)));
  }

  /**
   * Accepts both the paginated contract and the previous list contract while local backend instances are refreshed.
   */
  private normalizeFeedPage(
    response: ArticleFeedApiResponse,
    page: number,
    size: number,
  ): PageResponse<ArticleFeedItem> {
    if (Array.isArray(response)) {
      const start = page * size;
      const content = response.slice(start, start + size);

      return {
        content,
        page,
        size,
        totalElements: response.length,
        totalPages: Math.ceil(response.length / size),
        first: page === 0,
        last: start + size >= response.length,
      };
    }

    return {
      ...response,
      content: response.content ?? [],
      page: response.page ?? page,
      size: response.size ?? size,
      totalElements: response.totalElements ?? response.content?.length ?? 0,
      totalPages: response.totalPages ?? 1,
      first: response.first ?? page === 0,
      last: response.last ?? true,
    };
  }

  /**
   * Creates a new article for the authenticated user.
   */
  createArticle(payload: CreateArticlePayload): Observable<ArticleDetail> {
    return this.authService.authenticatedPost<ArticleDetail>(`${environment.apiUrl}/posts`, payload);
  }

  /**
   * Loads one article by id.
   */
  loadArticle(articleId: string): Observable<ArticleDetail> {
    return this.authService.authenticatedGet<ArticleDetail>(`${environment.apiUrl}/posts/${articleId}`);
  }

  /**
   * Adds a comment to an article.
   */
  addComment(articleId: string, payload: CreateCommentPayload): Observable<ArticleComment> {
    return this.authService.authenticatedPost<ArticleComment>(
      `${environment.apiUrl}/posts/${articleId}/comments`,
      payload,
    );
  }
}
