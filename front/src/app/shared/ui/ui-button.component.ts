import { Component, EventEmitter, Output, input } from '@angular/core';

@Component({
  selector: 'app-ui-button',
  standalone: true,
  template: `
    <button
      [attr.type]="buttonType()"
      class="ui-button"
      [class.ui-button-outline]="variant() === 'outline'"
      [class.ui-button-primary]="variant() === 'primary'"
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

      .ui-button:disabled {
        opacity: 0.7;
        cursor: wait;
      }
    `,
  ],
})
export class UiButtonComponent {
  readonly variant = input<'primary' | 'outline'>('primary');
  readonly buttonType = input<'button' | 'submit'>('button');
  readonly disabled = input(false);

  @Output() readonly buttonClick = new EventEmitter<void>();
}
