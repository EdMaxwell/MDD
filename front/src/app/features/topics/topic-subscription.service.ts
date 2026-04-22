import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

/** Topic catalog item enriched with the current user's subscription state. */
export interface TopicItem {
  id: string;
  name: string;
  description?: string;
  subscribed: boolean;
}

/**
 * Provides authenticated topic catalog and subscription API calls.
 */
@Injectable({ providedIn: 'root' })
export class TopicSubscriptionService {
  private readonly authService = inject(AuthService);

  /**
   * Loads all topics with their current subscription state.
   */
  loadTopics(): Observable<TopicItem[]> {
    return this.authService.authenticatedGet<TopicItem[]>(`${environment.apiUrl}/topics`);
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
