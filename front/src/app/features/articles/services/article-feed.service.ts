import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../../core/auth/auth.service';
import {
  PageResponse,
  PaginatedApiResponse,
  normalizePageResponse,
} from '../../../shared/pagination/page-response';

/** Supported creation-date sort directions for the article feed. */
export type ArticleSortDirection = 'desc' | 'asc';

/** Default number of article cards displayed per feed page. */
export const DEFAULT_ARTICLE_FEED_PAGE_SIZE = 6;

/** Page-size choices exposed by the feed paginator. */
export const ARTICLE_FEED_PAGE_SIZE_OPTIONS = [6, 8, 10, 12] as const;

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

type ArticleFeedApiResponse = PaginatedApiResponse<ArticleFeedItem>;

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
  loadFeed(sort: ArticleSortDirection, page = 0, size = DEFAULT_ARTICLE_FEED_PAGE_SIZE): Observable<PageResponse<ArticleFeedItem>> {
    const params = new URLSearchParams({
      sort,
      page: String(page),
      size: String(size),
    });
    return this.authService
      .authenticatedGet<ArticleFeedApiResponse>(`${environment.apiUrl}/posts/feed?${params}`)
      .pipe(map((response) => normalizePageResponse(response, page, size)));
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
