import { Component, input, output } from '@angular/core';
import { UiButtonComponent } from '../../shared/ui/ui-button/ui-button.component';
import { TopicItem } from './topic-subscription.service';

/**
 * Displays one topic and emits subscribe requests from the catalog page.
 */
@Component({
  selector: 'app-topic-card',
  standalone: true,
  imports: [UiButtonComponent],
  template: `
    <article class="topic-card">
      <h2>{{ topic().name }}</h2>
      <p>
        Description:
        {{ topic().description || 'Aucune description disponible pour le moment.' }}
      </p>

      <app-ui-button
        [variant]="topic().subscribed ? 'muted' : 'primary'"
        [disabled]="topic().subscribed || loading()"
        (buttonClick)="subscribe.emit(topic().id)"
      >
        {{ topic().subscribed ? 'Deja abonne' : loading() ? 'Abonnement...' : "S'abonner" }}
      </app-ui-button>
    </article>
  `,
  styles: [
    `
      .topic-card {
        display: grid;
        grid-template-rows: auto 1fr auto;
        gap: 0.85rem;
        min-height: 7.2rem;
        padding: 0.75rem 1rem 0.7rem;
        border-radius: 8px;
        background: #f5f5f5;
        overflow: hidden;
      }

      h2,
      p {
        margin: 0;
      }

      h2 {
        font-size: 1rem;
        line-height: 1.2;
        font-weight: 700;
      }

      p {
        font-size: 0.95rem;
        line-height: 1.16;
        display: -webkit-box;
        overflow: hidden;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 3;
      }

      app-ui-button {
        justify-self: center;
      }

      @media (max-width: 960px) {
        .topic-card {
          min-height: 7.95rem;
          padding: 0.95rem 0.95rem 0.85rem;
        }

        p {
          font-size: 0.8rem;
          line-height: 1.13;
          -webkit-line-clamp: 4;
        }
      }
    `,
  ],
})
export class TopicCardComponent {
  /** Topic data rendered by the card. */
  readonly topic = input.required<TopicItem>();
  /** Whether a subscription request is running for this card. */
  readonly loading = input(false);
  /** Emits the selected topic id when the user requests a subscription. */
  readonly subscribe = output<string>();
}
