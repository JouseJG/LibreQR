import { describe, expect, it, vi } from 'vitest';
import { ListQueryState } from './list-query-state';

describe('ListQueryState', () => {
  it('cualquier filtro devuelve la pagina a 1 y recarga', () => {
    let reloads = 0;
    const list = new ListQueryState({ reload: () => reloads++ });
    list.setPage(4);
    expect(list.page()).toBe(4);
    expect(reloads).toBe(1); // el constructor no recarga: lo hace la pagina

    list.setSort('name,asc');
    expect(list.page()).toBe(1);
    expect(list.sort()).toBe('name,asc');

    list.setStatus('active');
    expect(list.page()).toBe(1);
    expect(list.statusTab()).toBe('active');
  });

  it('la busqueda usa debounce y no dispara reloads por tecleo', () => {
    vi.useFakeTimers();
    let reloads = 0;
    const list = new ListQueryState({ reload: () => reloads++, debounceMs: 300 });
    list.setSearch('ca');
    list.setSearch('car');
    list.setSearch('cartel');
    expect(reloads).toBe(0);
    vi.advanceTimersByTime(300);
    expect(reloads).toBe(1);
    expect(list.search()).toBe('cartel');
    vi.useRealTimers();
  });

  it('expone parametros null cuando estan vacios', () => {
    const list = new ListQueryState({ reload: () => undefined });
    expect(list.searchParam()).toBeUndefined();
    expect(list.statusParam()).toBeUndefined();
    list.setSearch('  hola  ');
    list.setStatus('paused');
    expect(list.searchParam()).toBe('hola');
    expect(list.statusParam()).toBe('paused');
  });
});
