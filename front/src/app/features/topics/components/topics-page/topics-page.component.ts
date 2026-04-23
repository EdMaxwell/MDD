import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { PaginatedCardGridComponent } from '../../../../shared/ui/paginated-card-grid/paginated-card-grid.component';
import { TopbarComponent } from '../../../../shared/ui/topbar/topbar.component';
import { TopicCardComponent } from '../topic-card/topic-card.component';
import {
  DEFAULT_TOPIC_PAGE_SIZE,
  TOPIC_PAGE_SIZE_OPTIONS,
  TopicItem,
  TopicSubscriptionService,
} from '../../topic-subscription.service';
import { PaginatorState } from 'primeng/types/paginator';

/**
 * Displays the topic catalog and lets authenticated users subscribe to topics.
 */
@Component({
  selector: 'app-topics-page',
  standalone: true,
  imports: [CommonModule, TopbarComponent, TopicCardComponent, PaginatedCardGridComponent],
  templateUrl: './topics-page.component.html',
  styleUrl: './topics-page.component.scss',
})
export class TopicsPageComponent {
  protected readonly pageSizeOptions = [...TOPIC_PAGE_SIZE_OPTIONS];
  protected readonly authService = inject(AuthService);
  private readonly topicSubscriptionService = inject(TopicSubscriptionService);
  private readonly router = inject(Router);

  protected readonly topics = signal<TopicItem[]>([]);
  protected readonly loadingTopics = signal(true);
  protected readonly topicsError = signal('');
  protected readonly updatingTopicIds = signal<Set<string>>(new Set());
  protected readonly currentPage = signal(0);
  protected readonly pageSize = signal(DEFAULT_TOPIC_PAGE_SIZE);
  protected readonly totalTopics = signal(0);

  constructor() {
    this.authService.init();
    this.loadTopics();

    effect(() => {
      const isCheckingSession = this.authService.checkingSession();
      const isAuthenticated = this.authService.isAuthenticated();

      if (!isAuthenticated && !isCheckingSession) {
        this.router.navigateByUrl('/');
      }
    });
  }

  /**
   * Loads the topic page selected from the shared paginator.
   */
  protected changePage(event: PaginatorState): void {
    this.pageSize.set(event.rows ?? DEFAULT_TOPIC_PAGE_SIZE);
    this.currentPage.set(event.page ?? 0);
    this.loadTopics();
  }

  /**
   * Checks whether a subscription request is already running for a topic.
   */
  protected isUpdating(topicId: string): boolean {
    return this.updatingTopicIds().has(topicId);
  }

  /**
   * Subscribes to a topic unless the card is already subscribed or being updated.
   */
  protected subscribe(topicId: string): void {
    const topic = this.topics().find((item) => item.id === topicId);
    if (!topic || topic.subscribed || this.isUpdating(topicId)) {
      return;
    }

    this.topicsError.set('');
    this.trackUpdating(topicId, true);
    this.topicSubscriptionService.subscribe(topicId).subscribe({
      next: (updatedTopic) => {
        this.replaceTopic(updatedTopic);
        this.trackUpdating(topicId, false);
      },
      error: (error: HttpErrorResponse) => {
        this.trackUpdating(topicId, false);
        this.topicsError.set(this.resolveTopicsError(error));
      },
    });
  }

  /**
   * Loads the topic catalog for the authenticated user.
   */
  private loadTopics(): void {
    this.loadingTopics.set(true);
    this.topicsError.set('');

    this.topicSubscriptionService.loadTopics(this.currentPage(), this.pageSize()).subscribe({
      next: (page) => {
        if (page.content.length === 0 && page.totalElements > 0 && this.currentPage() > 0) {
          this.currentPage.set(0);
          this.loadTopics();
          return;
        }

        this.topics.set(page.content);
        this.currentPage.set(page.page);
        this.totalTopics.set(page.totalElements);
        this.loadingTopics.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.loadingTopics.set(false);
        this.topicsError.set(this.resolveTopicsError(error));
      },
    });
  }

  /**
   * Replaces one topic in the local list after the backend returns the updated state.
   */
  private replaceTopic(updatedTopic: TopicItem): void {
    this.topics.update((topics) =>
      topics.map((topic) => (topic.id === updatedTopic.id ? updatedTopic : topic)),
    );
  }

  /**
   * Tracks in-flight topic updates without mutating the existing signal value.
   */
  private trackUpdating(topicId: string, updating: boolean): void {
    this.updatingTopicIds.update((topicIds) => {
      const nextTopicIds = new Set(topicIds);
      if (updating) {
        nextTopicIds.add(topicId);
      } else {
        nextTopicIds.delete(topicId);
      }
      return nextTopicIds;
    });
  }

  /**
   * Maps topic API failures to user-facing messages.
   */
  private resolveTopicsError(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Le backend est inaccessible. Verifie que Spring Boot tourne sur le port 8080.';
    }

    if (error.status === 401 || error.status === 403) {
      return 'La session a expire. Reconnecte-toi pour consulter les themes.';
    }

    if (error.status === 404) {
      return "Ce theme n'existe plus.";
    }

    return 'Impossible de charger les themes pour le moment.';
  }
}
