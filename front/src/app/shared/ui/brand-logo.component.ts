import { Component, input } from '@angular/core';

@Component({
  selector: 'app-brand-logo',
  standalone: true,
  template: '<img class="brand-logo" [class]="sizeClass()" src="/assets/logo_p6.png" alt="MDD" />',
  styles: [
    `
      .brand-logo {
        display: block;
        width: 100%;
        height: auto;
      }

      .logo-large {
        max-width: 320px;
      }

      .logo-medium {
        max-width: 180px;
      }

      .logo-small {
        max-width: 82px;
      }
    `,
  ],
})
export class BrandLogoComponent {
  readonly size = input<'small' | 'medium' | 'large'>('medium');

  protected sizeClass(): string {
    return `brand-logo logo-${this.size()}`;
  }
}
