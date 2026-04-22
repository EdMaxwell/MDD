import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

export type ArticleSortDirection = 'desc' | 'asc';

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

export interface ArticleComment {
  id: string;
  content: string;
  createdAt: string;
  authorId: string;
  authorName: string;
}

export interface ArticleDetail extends ArticleFeedItem {
  comments: ArticleComment[];
}

export interface CreateArticlePayload {
  topicId: string;
  title: string;
  content: string;
}

export interface CreateCommentPayload {
  content: string;
}

@Injectable({ providedIn: 'root' })
export class ArticleFeedService {
  private readonly authService = inject(AuthService);

  loadFeed(sort: ArticleSortDirection): Observable<ArticleFeedItem[]> {
    const params = new URLSearchParams({ sort });
    return this.authService.authenticatedGet<ArticleFeedItem[]>(`${environment.apiUrl}/posts/feed?${params}`);
  }

  createArticle(payload: CreateArticlePayload): Observable<ArticleDetail> {
    return this.authService.authenticatedPost<ArticleDetail>(`${environment.apiUrl}/posts`, payload);
  }

  loadArticle(articleId: string): Observable<ArticleDetail> {
    return this.authService.authenticatedGet<ArticleDetail>(`${environment.apiUrl}/posts/${articleId}`);
  }

  addComment(articleId: string, payload: CreateCommentPayload): Observable<ArticleComment> {
    return this.authService.authenticatedPost<ArticleComment>(
      `${environment.apiUrl}/posts/${articleId}/comments`,
      payload,
    );
  }
}
