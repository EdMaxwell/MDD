import { Component, input, output } from '@angular/core';

/**
 * Small reusable button component with project variants and submit support.
 */
@Component({
  selector: 'app-ui-button',
  standalone: true,
  templateUrl: './ui-button.component.html',
  styleUrl: './ui-button.component.scss',
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
