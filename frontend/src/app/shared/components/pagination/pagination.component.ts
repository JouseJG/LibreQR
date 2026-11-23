import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { pageWindow, totalPages as countPages } from '../../../core/utils/pagination';

@Component({
  selector: 'app-pagination',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (totalPages() > 1) {
      <nav class="pagination" aria-label="Paginación de resultados">
        <span class="pagination__count">
          {{ rangeLabel() }} de {{ totalItems() }}
        </span>

        <div class="pagination__pages">
          <button
            type="button"
            class="pagination__button"
            [disabled]="page() <= 1"
            aria-label="Página anterior"
            (click)="goToPage(page() - 1)">
            ‹
          </button>

          @for (item of window(); track $index) {
            @if (item === 'ellipsis') {
              <span class="pagination__ellipsis" aria-hidden="true">…</span>
            } @else {
              <button
                type="button"
                class="pagination__button pagination__number"
                [class.is-current]="item === page()"
                [attr.aria-current]="item === page() ? 'page' : null"
                [attr.aria-label]="'Página ' + item"
                (click)="goToPage(item)">
                {{ item }}
              </button>
            }
          }

          <button
            type="button"
            class="pagination__button"
            [disabled]="page() >= totalPages()"
            aria-label="Página siguiente"
            (click)="goToPage(page() + 1)">
            ›
          </button>
        </div>
      </nav>
    }
  `,
  styles: `
    @use 'tokens' as *;

    .pagination {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      flex-wrap: wrap;
      padding: 0.9rem 1.2rem;
      border-top: 1px solid $color-border;
    }

    .pagination__count {
      color: $color-text-muted;
      font-size: 0.82rem;
    }

    .pagination__pages {
      display: flex;
      align-items: center;
      gap: 0.35rem;
    }

    .pagination__button {
      min-width: 38px;
      height: 38px;
      border-radius: 10px;
      border: 1px solid $color-border;
      background: $color-surface;
      color: $color-text;
      font-weight: 600;
      font-size: 0.88rem;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }

    .pagination__button:disabled {
      opacity: 0.45;
      cursor: not-allowed;
    }

    .pagination__button.is-current,
    .pagination__button:hover:not(:disabled) {
      border-color: $color-primary;
      color: $color-primary;
    }

    .pagination__button.is-current {
      background: $color-primary-soft;
    }

    .pagination__ellipsis {
      padding: 0 0.25rem;
      color: $color-text-muted;
    }
  `,
})
export class PaginationComponent {
  readonly page = input(1);
  readonly pageSize = input(10);
  readonly totalItems = input(0);

  readonly pageChange = output<number>();

  protected readonly totalPages = computed(() => countPages(this.totalItems(), this.pageSize()));
  protected readonly window = computed(() => pageWindow(this.page(), this.totalPages()));

  protected readonly rangeLabel = computed(() => {
    const start = (this.page() - 1) * this.pageSize() + 1;
    const end = Math.min(this.page() * this.pageSize(), this.totalItems());
    return `${start}–${end}`;
  });

  protected goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages() && page !== this.page()) {
      this.pageChange.emit(page);
    }
  }
}
