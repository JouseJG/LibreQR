import {
  clampPage,
  pageWindow,
  slicePage,
  totalPages,
} from './pagination';

describe('pagination utils', () => {
  describe('slicePage', () => {
    const items = Array.from({ length: 12 }, (_, i) => i + 1);

    it('devuelve la primera página', () => {
      expect(slicePage(items, 1, 5)).toEqual([1, 2, 3, 4, 5]);
    });

    it('devuelve páginas intermedias', () => {
      expect(slicePage(items, 2, 5)).toEqual([6, 7, 8, 9, 10]);
    });

    it('recorta la última página incompleta', () => {
      expect(slicePage(items, 3, 5)).toEqual([11, 12]);
    });

    it('devuelve un array vacío si la página está fuera de rango', () => {
      expect(slicePage(items, 4, 5)).toEqual([]);
    });
  });

  describe('totalPages', () => {
    it('redondea hacia arriba', () => {
      expect(totalPages(11, 5)).toBe(3);
    });

    it('es 1 aunque no haya elementos', () => {
      expect(totalPages(0, 10)).toBe(1);
    });
  });

  describe('clampPage', () => {
    it('limita la página al total existente', () => {
      expect(clampPage(8, 3)).toBe(3);
      expect(clampPage(0, 3)).toBe(1);
      expect(clampPage(2, 3)).toBe(2);
    });
  });

  describe('pageWindow', () => {
    it('muestra todas las páginas si caben', () => {
      expect(pageWindow(2, 5)).toEqual([1, 2, 3, 4, 5]);
    });

    it('colapsa los huecos con ellipsis cerca del inicio', () => {
      expect(pageWindow(1, 10)).toEqual([1, 2, 'ellipsis', 10]);
    });

    it('muestra vecinos y extremos desde el centro', () => {
      expect(pageWindow(5, 10)).toEqual([1, 'ellipsis', 4, 5, 6, 'ellipsis', 10]);
    });

    it('colapsa los huecos cerca del final', () => {
      expect(pageWindow(10, 10)).toEqual([1, 'ellipsis', 9, 10]);
    });
  });
});
