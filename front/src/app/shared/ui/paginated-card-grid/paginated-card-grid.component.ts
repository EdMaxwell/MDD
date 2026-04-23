import { Component, input, output } from '@angular/core';
import { PaginatorModule } from 'primeng/paginator';
import { PaginatorState } from 'primeng/types/paginator';

/**
 * Reusable responsive card grid with an optional PrimeNG paginator.
 */
@Component({
  selector: 'app-paginated-card-grid',
  standalone: true,
  imports: [PaginatorModule],
  templateUrl: './paginated-card-grid.component.html',
  styleUrl: './paginated-card-grid.component.scss',
})
export class PaginatedCardGridComponent {
  /** Accessible label forwarded to the card grid region. */
  readonly ariaLabel = input.required<string>();
  /** Current zero-based page index. */
  readonly currentPage = input.required<number>();
  /** Current number of items requested per page. */
  readonly pageSize = input.required<number>();
  /** Selectable page-size options displayed by the paginator. */
  readonly pageSizeOptions = input.required<number[]>();
  /** Total number of items available across every page. */
  readonly totalRecords = input.required<number>();
  /** Row gap applied between card rows. */
  readonly rowGap = input('1.5rem');
  /** Column gap applied between card columns. */
  readonly columnGap = input('2rem');
  /** Padding applied around the desktop grid. */
  readonly gridPadding = input('2.5rem 1.45rem');
  /** Gap applied by the single-column mobile grid. */
  readonly mobileGap = input('1.1rem');
  /** Padding applied around the mobile grid. */
  readonly mobileGridPadding = input('0 0.55rem 2rem');

  /** Emits PrimeNG page-change events to the feature component. */
  readonly pageChange = output<PaginatorState>();
}
