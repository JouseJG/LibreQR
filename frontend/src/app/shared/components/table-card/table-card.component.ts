import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { PaginationComponent } from '../pagination/pagination.component';

/**
 * Armazon reutilizable de tabla: marco de estilo, estados de carga/error/vacio y
 * paginacion opcional. La pagina proyecta su propio <table> como contenido.
 *
 * <app-table-card [loading]="..." [error]="..." [empty]="..."
 *                 [totalItems]="..." [page]="..." (retry) (pageChange)>
 *   <table>...</table>
 * </app-table-card>
 */
@Component({
  selector: 'app-table-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [PaginationComponent],
  template: `
    <div class="table-card" [class.table-card-bare]="!framed()">
      @if (loading()) {
        <p class="table-state" role="status">{{ loadingText() }}</p>
      } @else if (error()) {
        <p class="table-state table-state--error" role="alert">
          {{ error() }}
          <button type="button" class="retry-link" (click)="retry.emit()">Reintentar</button>
        </p>
      } @else if (empty()) {
        <p class="table-state">{{ emptyText() }}</p>
      } @else {
        <ng-content />
      }

      @if (totalItems() !== null) {
        <app-pagination
          [page]="page()"
          [pageSize]="pageSize()"
          [totalItems]="totalItems() ?? 0"
          (pageChange)="pageChange.emit($event)"></app-pagination>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .table-state {
      text-align: center;
      color: var(--color-text-muted);
      padding: 2rem 1rem;
      margin: 0;
      font-size: 0.95rem;
    }

    .table-state--error {
      color: var(--color-danger);
    }

    .retry-link {
      background: none;
      border: none;
      padding: 0;
      margin-left: 0.5rem;
      color: var(--color-primary);
      font-weight: 600;
      font-size: inherit;
      cursor: pointer;
      text-decoration: underline;
    }
  `,
})
export class TableCardComponent {
  /** false = sin marco propio (lo aporta un contenedor externo, p. ej. app-data-table). */
  readonly framed = input(true);

  readonly loading = input(false);
  readonly loadingText = input('Cargando…');
  readonly error = input('');
  readonly empty = input(false);
  readonly emptyText = input('No se han encontrado resultados.');
  /** null = sin paginacion. */
  readonly totalItems = input<number | null>(null);
  readonly page = input(1);
  readonly pageSize = input(10);

  readonly retry = output<void>();
  readonly pageChange = output<number>();
}
