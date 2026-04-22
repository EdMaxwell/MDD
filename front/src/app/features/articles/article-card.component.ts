import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { ArticleFeedItem } from './article-feed.service';

/**
 * Displays a compact, keyboard-accessible article preview for feed grids.
 */
@Component({
  selector: 'app-article-card',
  standalone: true,
  imports: [DatePipe],
  template: `
    <article
      class="article-card"
      role="button"
      tabindex="0"
      (click)="articleClick.emit(article().id)"
      (keydown.enter)="articleClick.emit(article().id)"
      (keydown.space)="selectFromSpace($event)"
    >
      <h2>{{ article().title }}</h2>

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

      <p>{{ article().content }}</p>
    </article>
  `,
  styles: [
    `
      .article-card {
        display: grid;
        gap: 0.85rem;
        min-height: 11.8rem;
        padding: 1rem 1.1rem 1.15rem;
        border-radius: 8px;
        background: #f5f5f5;
        overflow: hidden;
        cursor: pointer;
      }

      .article-card:focus-visible {
        outline: 2px solid #7763da;
        outline-offset: 3px;
      }

      h2,
      dl,
      dd,
      p {
        margin: 0;
      }

      h2 {
        font-size: 1.1rem;
        line-height: 1.2;
        font-weight: 700;
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
        -webkit-line-clamp: 4;
      }

      @media (max-width: 768px) {
        .article-card {
          min-height: 8.65rem;
          gap: 0.55rem;
          padding: 0.65rem 0.95rem 0.85rem;
        }

        h2 {
          font-size: 1rem;
        }

        dd,
        p {
          font-size: 0.95rem;
          line-height: 1.12;
        }

        p {
          -webkit-line-clamp: 4;
        }
      }
    `,
  ],
})
export class ArticleCardComponent {
  /** Article data rendered by the card. */
  readonly article = input.required<ArticleFeedItem>();

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
