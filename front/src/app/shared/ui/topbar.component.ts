import { Component } from '@angular/core';
import { BrandLogoComponent } from './brand-logo.component';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [BrandLogoComponent],
  template: `
    <header class="topbar">
      <app-brand-logo size="small" />
    </header>
  `,
  styles: [
    `
      .topbar {
        display: flex;
        align-items: center;
        padding: 0 1rem;
        min-height: 52px;
      }
    `,
  ],
})
export class TopbarComponent {}
