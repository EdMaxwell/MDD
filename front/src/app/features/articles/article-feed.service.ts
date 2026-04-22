import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
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
   * Loads the current user's feed from followed topics.
   */
  loadFeed(sort: ArticleSortDirection): Observable<ArticleFeedItem[]> {
    const params = new URLSearchParams({ sort });
    return this.authService.authenticatedGet<ArticleFeedItem[]>(`${environment.apiUrl}/posts/feed?${params}`);
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
