import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  TemplateRef,
  contentChild,
  input,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { ListQueryState } from '../../../core/utils/list-query-state';
import { FilterOption, ListFiltersComponent } from '../list-filters/list-filters.component';
import { TableCardComponent } from '../table-card/table-card.component';
import { UiTabsComponent } from '../ui-tabs/ui-tabs.component';

export type TableRowContext<T> = { $implicit: T };

export type ListTab = { key: string; label: string };

/**
 * Vista de listado completa: barra de filtros (buscador + orden + pestanas de
 * estado), tabla con cabecera configurable, estados carga/error/vacio y
 * paginacion. Bebe directamente de un `ListQueryState`, de modo que la pagina
 * solo aporta las filas, las columnas y la plantilla de cada fila:
 *
 * <app-data-table [query]="list" [rows]="rows()" [total]="total()"
 *                 [columns]="['Nombre', 'Estado']" [rowLink]="rowLink"
 *                 [tabs]="tabs" tabsAriaLabel="Filtrar por estado">
 *   <ng-template #rowCells let-row><td>{{ row.name }}</td>…</ng-template>
 * </app-data-table>
 */
@Component({
  selector: 'app-data-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    NgTemplateOutlet,
    RouterLink,
    ListFiltersComponent,
    TableCardComponent,
    UiTabsComponent,
  ],
  template: `
    <div class="data-table-shell">
    <app-list-filters
      [framed]="false"
      [showSearch]="showSearch()"
      searchLabel="Buscar"
      [searchPlaceholder]="searchPlaceholder()"
      [search]="query().search()"
      (searchChange)="query().setSearch($event)"
      [sortOptions]="sortOptions()"
      [sortBy]="query().sort()"
      (sortByChange)="query().setSort($event)">
      @if (tabs().length) {
        <ui-tabs
          listTabs
          [ariaLabel]="tabsAriaLabel()"
          [tabs]="tabs()"
          [activeKey]="query().statusTab()"
          (tabChange)="query().setStatus($event)"></ui-tabs>
      }
    </app-list-filters>

    <app-table-card
      [framed]="false"
      [loading]="query().loading()"
      [error]="query().error()"
      [empty]="rows().length === 0"
      [emptyText]="emptyText()"
      [totalItems]="total()"
      [page]="query().page()"
      [pageSize]="pageSize()"
      (retry)="query().retry()"
      (pageChange)="query().setPage($event)">
      <table>
        <thead>
          <tr>
            @for (column of columns(); track $index) {
              <th>{{ column }}</th>
            }
          </tr>
        </thead>
        <tbody>
          @for (row of rows(); track rowTrackFn(row)) {
            <tr
              [class.row-click]="rowLink()"
              [routerLink]="rowLink() ? rowLink()!(row) : null">
              <ng-template
                [ngTemplateOutlet]="rowCells()"
                [ngTemplateOutletContext]="rowContext(row)"></ng-template>
            </tr>
          }
        </tbody>
      </table>
    </app-table-card>
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .data-table-shell {
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-soft);
      overflow: hidden;
    }

    .data-table-shell > * {
      margin: 0;
    }
  `,
})
export class DataTableComponent<T> {
  readonly query = input.required<ListQueryState>();
  readonly rows = input<T[]>([]);
  readonly columns = input<string[]>([]);
  readonly total = input<number | null>(null);
  readonly pageSize = input(5);
  readonly emptyText = input('No se han encontrado resultados.');

  readonly showSearch = input(true);
  readonly searchPlaceholder = input('');
  readonly sortOptions = input<FilterOption[]>([]);
  readonly tabs = input<ListTab[]>([]);
  readonly tabsAriaLabel = input('Filtrar por estado');

  /** Si se define, la fila entera navega (clase row-click incluida). */
  readonly rowLink = input<((row: T) => unknown[]) | null>(null);
  readonly trackBy = input<(row: T) => unknown>((row) => (row as { id: string }).id);

  readonly rowCells = contentChild<TemplateRef<TableRowContext<T>>>('rowCells');

  protected rowTrackFn = this.trackBy();

  protected rowContext(row: T): TableRowContext<T> {
    return { $implicit: row };
  }
}
