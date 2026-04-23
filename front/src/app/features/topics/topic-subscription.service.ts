import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';
import {
  PageResponse,
  PaginatedApiResponse,
  normalizePageResponse,
} from '../../shared/pagination/page-response';

/** Default number of topic cards displayed per catalog page. */
export const DEFAULT_TOPIC_PAGE_SIZE = 6;

/** Page-size choices exposed by the topic paginator. */
export const TOPIC_PAGE_SIZE_OPTIONS = [6, 8, 10, 12] as const;

/** Upper bound aligned with the backend topic page-size guard. */
const TOPIC_OPTIONS_PAGE_SIZE = 24;

/** Topic catalog item enriched with the current user's subscription state. */
export interface TopicItem {
  id: string;
  name: string;
  description?: string;
  subscribed: boolean;
}

type TopicApiResponse = PaginatedApiResponse<TopicItem>;

/**
 * Provides authenticated topic catalog and subscription API calls.
 */
@Injectable({ providedIn: 'root' })
export class TopicSubscriptionService {
  private readonly authService = inject(AuthService);

  /**
   * Loads one topic catalog page with the current subscription state.
   */
  loadTopics(page = 0, size = DEFAULT_TOPIC_PAGE_SIZE): Observable<PageResponse<TopicItem>> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    });
    return this.authService
      .authenticatedGet<TopicApiResponse>(`${environment.apiUrl}/topics?${params}`)
      .pipe(map((response) => normalizePageResponse(response, page, size)));
  }

  /**
   * Loads topic options for forms that need a compact selector rather than a paginated grid.
   */
  loadTopicOptions(): Observable<TopicItem[]> {
    return this.loadTopics(0, TOPIC_OPTIONS_PAGE_SIZE).pipe(map((page) => page.content));
  }

  /**
   * Subscribes the authenticated user to a topic.
   */
  subscribe(topicId: string): Observable<TopicItem> {
    return this.authService.authenticatedPost<TopicItem>(
      `${environment.apiUrl}/topics/${topicId}/subscription`,
      {},
    );
  }

  /**
   * Unsubscribes the authenticated user from a topic.
   */
  unsubscribe(topicId: string): Observable<TopicItem> {
    return this.authService.authenticatedDelete<TopicItem>(
      `${environment.apiUrl}/topics/${topicId}/subscription`,
    );
  }
}
