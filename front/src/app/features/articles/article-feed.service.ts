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

@Injectable({ providedIn: 'root' })
export class ArticleFeedService {
  private readonly authService = inject(AuthService);

  loadFeed(sort: ArticleSortDirection): Observable<ArticleFeedItem[]> {
    const params = new URLSearchParams({ sort });
    return this.authService.authenticatedGet<ArticleFeedItem[]>(`${environment.apiUrl}/posts/feed?${params}`);
  }
}
