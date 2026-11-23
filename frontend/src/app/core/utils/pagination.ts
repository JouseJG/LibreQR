export type PageWindowItem = number | 'ellipsis';

export function slicePage<T>(items: T[], page: number, pageSize: number): T[] {
  const start = (page - 1) * pageSize;
  return items.slice(start, start + pageSize);
}

export function totalPages(totalItems: number, pageSize: number): number {
  return Math.max(1, Math.ceil(totalItems / pageSize));
}

export function clampPage(page: number, total: number): number {
  return Math.min(Math.max(1, page), total);
}

export function pageWindow(current: number, total: number): PageWindowItem[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1);
  }

  const items: PageWindowItem[] = [];

  for (let page = 1; page <= total; page += 1) {
    const isVisible =
      page === 1 || page === total || Math.abs(page - current) <= 1;

    if (isVisible) {
      items.push(page);
    } else if (items[items.length - 1] !== 'ellipsis') {
      items.push('ellipsis');
    }
  }

  return items;
}
