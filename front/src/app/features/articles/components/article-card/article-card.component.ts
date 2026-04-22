import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { CardModule } from 'primeng/card';
import { CardPassThrough } from 'primeng/types/card';
import { ArticleFeedItem } from '../../services/article-feed.service';

/**
 * Displays a compact, keyboard-accessible article preview for feed grids.
 */
@Component({
  selector: 'app-article-card',
  standalone: true,
  imports: [DatePipe, CardModule],
  templateUrl: './article-card.component.html',
  styleUrl: './article-card.component.scss',
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
