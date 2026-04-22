import { Component, input, output } from '@angular/core';

/**
 * Small reusable button component with project variants and submit support.
 */
@Component({
  selector: 'app-ui-button',
  standalone: true,
  template: `
    <button
      [attr.type]="buttonType()"
      class="ui-button"
      [class.ui-button-outline]="variant() === 'outline'"
      [class.ui-button-primary]="variant() === 'primary'"
      [class.ui-button-muted]="variant() === 'muted'"
      [disabled]="disabled()"
      (click)="buttonClick.emit()"
    >
      <ng-content />
    </button>
  `,
  styles: [
    `
      .ui-button {
        min-width: 110px;
        min-height: 31px;
        padding: 0.45rem 1rem;
        border-radius: 5px;
        font-size: 12px;
        line-height: 1;
        cursor: pointer;
        transition: opacity 120ms ease;
      }

      .ui-button-outline {
        border: 1px solid #8e8e8e;
        background: #ffffff;
        color: #121212;
      }

      .ui-button-primary {
        border: 0;
        background: #7763da;
        color: #ffffff;
      }

      .ui-button-muted {
        border: 0;
        background: #939393;
        color: #ffffff;
      }

      .ui-button:disabled {
        cursor: default;
      }
    `,
  ],
})
export class UiButtonComponent {
  /** Visual style variant. */
  readonly variant = input<'primary' | 'outline' | 'muted'>('primary');
  /** Native button type attribute. */
  readonly buttonType = input<'button' | 'submit'>('button');
  /** Disabled state forwarded to the native button. */
  readonly disabled = input(false);

  /** Emits when the native button is clicked. */
  readonly buttonClick = output<void>();
}
