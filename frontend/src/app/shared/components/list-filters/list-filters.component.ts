import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

export type FilterOption = {
  value: string;
  label: string;
};

/**
 * Barra de herramientas de listado. Las listas consumen ahora paginacion/orden de
 * servidor: el buscador es opcional (la API aun no soporta busqueda por texto) y
 * las opciones de orden las aporta cada pagina (mapean a `?sort=` de Spring).
 */
@Component({
  selector: 'app-list-filters',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  template: `
    <div class="toolbar-panel">
      <ng-content select="[listTabs]" />

      <div class="toolbar-controls">
        @if (showSearch()) {
          <label class="search-field">
            <span>{{ searchLabel() }}</span>
            <input
              type="search"
              name="list-search"
              [placeholder]="searchPlaceholder()"
              [ngModel]="search()"
              (ngModelChange)="searchChange.emit($event)" />
          </label>
        }

        @if (filterOptions().length) {
          <label class="select-field">
            <span>{{ filterLabel() }}</span>
            <select name="list-filter" [ngModel]="filter()" (ngModelChange)="filterChange.emit($event)">
              <option value="all">{{ filterAllLabel() }}</option>
              @for (option of filterOptions(); track option.value) {
                <option [value]="option.value">{{ option.label }}</option>
              }
            </select>
          </label>
        }

        @if (sortOptions().length) {
          <label class="select-field">
            <span>Orden</span>
            <select name="list-sort" [ngModel]="sortBy()" (ngModelChange)="sortByChange.emit($event)">
              @for (option of sortOptions(); track option.value) {
                <option [value]="option.value">{{ option.label }}</option>
              }
            </select>
          </label>
        }
      </div>
    </div>
  `,
  styleUrl: './list-filters.component.scss',
})
export class ListFiltersComponent {
  readonly showSearch = input(true);
  readonly searchLabel = input('Buscar');
  readonly searchPlaceholder = input('');
  readonly search = input('');
  readonly searchChange = output<string>();

  readonly filterLabel = input('');
  readonly filterAllLabel = input('Todos');
  readonly filter = input('all');
  readonly filterChange = output<string>();
  readonly filterOptions = input<FilterOption[]>([]);

  readonly sortOptions = input<FilterOption[]>([]);
  readonly sortBy = input('');
  readonly sortByChange = output<string>();
}
