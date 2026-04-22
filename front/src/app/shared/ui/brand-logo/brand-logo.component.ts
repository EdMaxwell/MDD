import { Component, input } from '@angular/core';

/**
 * Renders the MDD logo with one of the supported responsive sizes.
 */
@Component({
  selector: 'app-brand-logo',
  standalone: true,
  templateUrl: './brand-logo.component.html',
  styleUrl: './brand-logo.component.scss',
})
export class BrandLogoComponent {
  /** Logo size variant. */
  readonly size = input<'small' | 'medium' | 'large'>('medium');

  /**
   * Builds the CSS class matching the current size input.
   */
  protected sizeClass(): string {
    return `brand-logo logo-${this.size()}`;
  }
}
