import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

export interface TopicItem {
  id: string;
  name: string;
  description?: string;
  subscribed: boolean;
}

@Injectable({ providedIn: 'root' })
export class TopicSubscriptionService {
  private readonly authService = inject(AuthService);

  loadTopics(): Observable<TopicItem[]> {
    return this.authService.authenticatedGet<TopicItem[]>(`${environment.apiUrl}/topics`);
  }

  subscribe(topicId: string): Observable<TopicItem> {
    return this.authService.authenticatedPost<TopicItem>(
      `${environment.apiUrl}/topics/${topicId}/subscription`,
      {},
    );
  }

  unsubscribe(topicId: string): Observable<TopicItem> {
    return this.authService.authenticatedDelete<TopicItem>(
      `${environment.apiUrl}/topics/${topicId}/subscription`,
    );
  }
}
