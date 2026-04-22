import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { CardModule } from 'primeng/card';
import { CardPassThrough } from 'primeng/types/card';
import { ArticleFeedItem } from './article-feed.service';

/**
 * Displays a compact, keyboard-accessible article preview for feed grids.
 */
@Component({
  selector: 'app-article-card',
  standalone: true,
  imports: [DatePipe, CardModule],
  template: `
    <p-card
      [pt]="cardPassThrough"
      (click)="articleClick.emit(article().id)"
      (keydown.enter)="articleClick.emit(article().id)"
      (keydown.space)="selectFromSpace($event)"
    >
      <ng-template #title>
        {{ article().title }}
      </ng-template>

      <ng-template #subtitle>
        <dl class="article-meta">
          <div>
            <dt>Date</dt>
            <dd>{{ article().createdAt | date: 'dd/MM/yyyy' }}</dd>
          </div>
          <div>
            <dt>Auteur</dt>
            <dd>{{ article().authorName }}</dd>
          </div>
        </dl>
      </ng-template>

      <p>{{ article().content }}</p>

      <ng-template #footer>
        <span class="article-topic">{{ article().topicName }}</span>
      </ng-template>
    </p-card>
  `,
  styles: [
    `
      :host {
        display: block;
        min-width: 0;
      }

      .article-card {
        display: grid;
        height: 10.75rem;
        max-height: 10.75rem;
        border: 0;
        border-radius: 8px;
        background: #f5f5f5;
        box-shadow: none;
        overflow: hidden;
        cursor: pointer;
      }

      .article-card:focus-visible {
        outline: 2px solid #7763da;
        outline-offset: 3px;
      }

      dl,
      dd,
      p {
        margin: 0;
      }

      :host ::ng-deep .article-card .p-card-body {
        display: grid;
        grid-template-rows: auto auto 1fr auto;
        gap: 0.5rem;
        height: 100%;
        padding: 0.75rem 1rem 0.85rem;
      }

      :host ::ng-deep .article-card .p-card-caption {
        gap: 0.45rem;
      }

      :host ::ng-deep .article-card .p-card-title {
        display: -webkit-box;
        overflow: hidden;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        color: #121212;
        font-size: 1.1rem;
        line-height: 1.2;
        font-weight: 700;
      }

      :host ::ng-deep .article-card .p-card-subtitle,
      :host ::ng-deep .article-card .p-card-content,
      :host ::ng-deep .article-card .p-card-footer {
        color: #121212;
      }

      :host ::ng-deep .article-card .p-card-content {
        padding: 0;
        overflow: hidden;
      }

      .article-meta {
        display: grid;
        grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
        gap: 1rem;
      }

      dt {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
        white-space: nowrap;
        border: 0;
      }

      dd,
      p {
        font-size: 1rem;
        line-height: 1.25;
      }

      p {
        display: -webkit-box;
        overflow: hidden;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
      }

      .article-topic {
        display: inline-flex;
        width: fit-content;
        max-width: 100%;
        padding: 0.2rem 0.55rem;
        border: 1px solid #7763da;
        border-radius: 999px;
        color: #5e47bd;
        font-size: 0.82rem;
        font-weight: 700;
        line-height: 1.2;
        overflow-wrap: anywhere;
      }

      @media (max-width: 960px) {
        .article-card {
          height: 9.65rem;
          max-height: 9.65rem;
        }

        :host ::ng-deep .article-card .p-card-body {
          gap: 0.4rem;
          padding: 0.6rem 0.85rem 0.7rem;
        }

        :host ::ng-deep .article-card .p-card-title {
          font-size: 1rem;
        }

        dd,
        p {
          font-size: 0.95rem;
          line-height: 1.12;
        }

        p {
          -webkit-line-clamp: 2;
        }
      }
    `,
  ],
})
export class ArticleCardComponent {
  /** Article data rendered by the card. */
  readonly article = input.required<ArticleFeedItem>();

  /** PrimeNG pass-through attributes applied to the Card root for accessibility and styling. */
  protected readonly cardPassThrough: CardPassThrough = {
    root: {
      class: 'article-card',
      role: 'button',
      tabindex: '0',
    },
  };

  /** Emits the selected article id when the card is clicked or activated from the keyboard. */
  readonly articleClick = output<string>();

  /**
   * Handles the Space key manually because the card uses article semantics with button behavior.
   *
   * @param event keyboard event raised by Angular's keydown binding
   */
  protected selectFromSpace(event: Event): void {
    event.preventDefault();
    this.articleClick.emit(this.article().id);
  }
}
