import { signal, type WritableSignal } from '@angular/core';

export type ListQueryOptions = {
  /** Relanza la consulta con el estado actual (el load() de la pagina). */
  reload: () => void;
  defaultSort?: string;
  debounceMs?: number;
};

/**
 * Estado comun de las listas del backend: pagina, orden, busqueda (con debounce),
 * pestana de estado y flags de carga/error. Centraliza la interaccion filtro<->
 * paginacion (cambiar cualquier filtro vuelve a la pagina 1) para que cada pagina
 * solo describa SU consulta.
 */
export class ListQueryState {
  readonly page: WritableSignal<number>;
  readonly sort: WritableSignal<string>;
  readonly search: WritableSignal<string>;
  readonly statusTab: WritableSignal<string>;
  readonly loading: WritableSignal<boolean>;
  readonly error: WritableSignal<string>;

  private readonly reload: () => void;
  private readonly debounceMs: number;
  private timer: ReturnType<typeof setTimeout> | null = null;

  constructor(options: ListQueryOptions) {
    this.reload = options.reload;
    this.debounceMs = options.debounceMs ?? 300;
    this.page = signal(1);
    this.sort = signal(options.defaultSort ?? 'createdAt,desc');
    this.search = signal('');
    this.statusTab = signal('all');
    this.loading = signal(true);
    this.error = signal('');
  }

  setPage(page: number): void {
    this.page.set(page);
    this.reload();
  }

  setSort(sort: string): void {
    this.sort.set(sort);
    this.page.set(1);
    this.reload();
  }

  setStatus(tab: string): void {
    this.statusTab.set(tab);
    this.page.set(1);
    this.reload();
  }

  /** Buscador con debounce: cada tecleo no abruma a la API. */
  setSearch(term: string): void {
    this.search.set(term);
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.timer = setTimeout(() => {
      this.page.set(1);
      this.reload();
    }, this.debounceMs);
  }

  retry(): void {
    this.reload();
  }

  /** Busqueda y filtro de estado listos para la querystring. */
  searchParam(): string | undefined {
    return this.search().trim() || undefined;
  }

  statusParam<T extends string>(): T | undefined {
    return this.statusTab() === 'all' ? undefined : (this.statusTab() as T);
  }
}
