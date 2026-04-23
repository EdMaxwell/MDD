import { Component, input, output } from '@angular/core';
import { CardModule } from 'primeng/card';
import { CardPassThrough } from 'primeng/types/card';
import { UiButtonComponent } from '../../../../shared/ui/ui-button/ui-button.component';
import { TopicItem } from '../../topic-subscription.service';

/**
 * Displays one topic and emits subscribe requests from the catalog page.
 */
@Component({
  selector: 'app-topic-card',
  standalone: true,
  imports: [CardModule, UiButtonComponent],
  templateUrl: './topic-card.component.html',
  styleUrl: './topic-card.component.scss',
})
export class TopicCardComponent {
  /** Topic data rendered by the card. */
  readonly topic = input.required<TopicItem>();
  /** Whether a subscription request is running for this card. */
  readonly loading = input(false);
  /** Emits the selected topic id when the user requests a subscription. */
  readonly subscribe = output<string>();

  /** PrimeNG pass-through attributes applied to the Card root for shared styling. */
  protected readonly cardPassThrough: CardPassThrough = {
    root: {
      class: 'topic-card',
    },
  };
}
