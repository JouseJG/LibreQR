import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';

export type DateRange = { from: string; to: string };

/** "5 – 19 nov 2026" · "5 nov – 3 dic 2026" · "5 nov 2025 – 3 feb 2026". */
export function formatRangeLabel(from: string, to: string): string {
  const f = parseIso(from);
  const t = parseIso(to);
  if (!f || !t || from === to) {
    return t ? FULL.format(t) : '';
  }
  if (f.getFullYear() === t.getFullYear() && f.getMonth() === t.getMonth()) {
    return `${DAY_ONLY.format(f)} – ${FULL.format(t)}`;
  }
  if (f.getFullYear() === t.getFullYear()) {
    return `${DAY_MONTH.format(f)} – ${FULL.format(t)}`;
  }
  return `${FULL.format(f)} – ${FULL.format(t)}`;
}

const FULL = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
const DAY_MONTH = new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' });
const DAY_ONLY = new Intl.DateTimeFormat('es-ES', { day: 'numeric' });
const WEEKDAYS = ['l', 'm', 'x', 'j', 'v', 's', 'd'];

function parseIso(value: string): Date | null {
  const [y, m, d] = value.split('-').map(Number);
  if (!y || !m || !d) {
    return null;
  }
  return new Date(y, m - 1, d);
}

function toIso(date: Date): string {
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${m}-${d}`;
}

type Cell = { iso: string; day: number } | null;

/**
 * Selector de rango de fechas: un unico campo que abre un calendario donde se
 * elige el dia inicial y el final (dos clics). Emite `rangeChange` al cerrar.
 */
@Component({
  selector: 'app-date-range',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="range-root">
      <button
        type="button"
        class="range-trigger"
        [class.open]="open()"
        [attr.aria-expanded]="open()"
        (click)="toggle()">
        <span class="range-icon" aria-hidden="true">📅</span>
        <span>{{ label() }}</span>
        <span class="range-caret" aria-hidden="true">▾</span>
      </button>

      @if (open()) {
        <div class="range-panel">
          <div class="panel-head">
            <button type="button" class="nav" aria-label="Mes anterior" (click)="shiftMonth(-1)">
              ‹
            </button>
            <strong>{{ viewLabel() }}</strong>
            <button type="button" class="nav" aria-label="Mes siguiente" (click)="shiftMonth(1)">
              ›
            </button>
          </div>
          <div class="weekdays" aria-hidden="true">
            @for (wd of weekdays; track wd) {
              <span>{{ wd }}</span>
            }
          </div>
          <div class="days">
            @for (cell of cells(); track cell?.iso ?? 'empty-' + $index) {
              @if (cell) {
                <button
                  type="button"
                  [class.today]="cell.iso === todayIso"
                  [class.selected]="isEndpoint(cell.iso)"
                  [class.in-range]="isInRange(cell.iso)"
                  [disabled]="isFuture(cell.iso)"
                  (click)="pick(cell.iso)">
                  {{ cell.day }}
                </button>
              } @else {
                <span class="pad"></span>
              }
            }
          </div>
          <div class="panel-foot">
            <span>
              {{ draftSummary() }}
            </span>
            <button type="button" class="reset" (click)="resetToCurrentMonth()">Este mes</button>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      display: inline-block;
      position: relative;
    }

    .range-trigger {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 4px 8px;
      border: none;
      border-radius: 8px;
      background: none;
      color: var(--color-text);
      font: inherit;
      font-size: 0.9rem;
      font-weight: 500;
      cursor: pointer;
    }

    .range-trigger:hover,
    .range-trigger.open {
      background: var(--color-primary-soft);
    }

    .range-icon {
      font-size: 0.95rem;
    }

    .range-caret {
      color: var(--color-text-muted);
      font-size: 0.7rem;
    }

    .range-panel {
      position: absolute;
      top: calc(100% + 6px);
      left: 0;
      z-index: 20;
      width: 272px;
      padding: 12px;
      border: 1px solid var(--color-border);
      border-radius: 14px;
      background: #fff;
      box-shadow: 0 12px 32px rgba(16, 24, 40, 0.16);
    }

    .panel-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;
      font-size: 0.9rem;
    }

    .nav {
      border: none;
      background: none;
      font-size: 1.1rem;
      line-height: 1;
      padding: 4px 10px;
      border-radius: 8px;
      cursor: pointer;
      color: var(--color-text-muted);
    }

    .nav:hover {
      background: #eef0f4;
    }

    .weekdays {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      text-align: center;
      font-size: 0.7rem;
      font-weight: 700;
      color: var(--color-text-muted);
      margin-bottom: 4px;
    }

    .days {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 2px;
    }

    .days button {
      border: none;
      background: none;
      font: inherit;
      font-size: 0.82rem;
      padding: 0;
      height: 32px;
      border-radius: 8px;
      cursor: pointer;
      color: var(--color-text);
    }

    .days button:hover:not(:disabled) {
      background: #eef0f4;
    }

    .days button:disabled {
      color: var(--color-text-muted);
      opacity: 0.4;
      cursor: default;
    }

    .days button.today {
      color: var(--color-primary);
    }

    .days button.in-range {
      background: var(--color-primary-soft);
      border-radius: 0;
    }

    .days button.selected {
      color: var(--color-primary);
      font-weight: 700;
      text-decoration: underline;
      text-decoration-color: var(--color-primary-soft);
      text-underline-offset: 3px;
      text-decoration-thickness: 3px;
    }

    .days .pad {
      height: 32px;
    }

    .panel-foot {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 10px;
      font-size: 0.75rem;
      color: var(--color-text-muted);
    }

    .reset {
      border: none;
      background: none;
      color: var(--color-primary);
      font: inherit;
      font-weight: 700;
      cursor: pointer;
      padding: 2px 4px;
    }
  `,
})
export class DateRangeComponent {
  readonly range = input.required<DateRange>();
  readonly rangeChange = output<DateRange>();

  protected readonly weekdays = WEEKDAYS;
  protected readonly todayIso = toIso(new Date());

  private readonly el = inject(ElementRef);

  protected readonly open = signal(false);
  private readonly view = signal({ y: new Date().getFullYear(), m0: new Date().getMonth() });
  private readonly draftFrom = signal<string | null>(null);
  private readonly draftTo = signal<string | null>(null);

  protected readonly label = computed(() =>
    formatRangeLabel(this.range().from, this.range().to),
  );

  protected readonly viewLabel = computed(() => {
    const { y, m0 } = this.view();
    return new Intl.DateTimeFormat('es-ES', { month: 'long', year: 'numeric' })
      .format(new Date(y, m0, 1))
      .replace(/^\w/, (c) => c.toUpperCase());
  });

  protected readonly cells = computed<Cell[]>(() => {
    const { y, m0 } = this.view();
    const offset = (new Date(y, m0, 1).getDay() + 6) % 7;
    const total = new Date(y, m0 + 1, 0).getDate();
    const list: Cell[] = [];
    for (let i = 0; i < offset; i += 1) {
      list.push(null);
    }
    for (let d = 1; d <= total; d += 1) {
      const date = new Date(y, m0, d);
      list.push({ iso: toIso(date), day: d });
    }
    return list;
  });

  protected draftSummary(): string {
    const from = this.draftFrom();
    const to = this.draftTo();
    if (!from) {
      return 'Elige el dia inicial';
    }
    if (!to) {
      return `Desde ${DAY_ONLY.format(parseIso(from)!)}`;
    }
    return formatRangeLabel(from, to);
  }

  protected isEndpoint(iso: string): boolean {
    return iso === this.draftFrom() || iso === this.draftTo();
  }

  protected isInRange(iso: string): boolean {
    const from = this.draftFrom();
    const to = this.draftTo();
    if (!from || !to) {
      return false;
    }
    return iso > from && iso < to;
  }

  protected isFuture(iso: string): boolean {
    return iso > this.todayIso;
  }

  protected toggle(): void {
    const next = !this.open();
    if (next) {
      this.draftFrom.set(this.range().from);
      this.draftTo.set(this.range().to);
      const from = parseIso(this.range().from);
      if (from) {
        this.view.set({ y: from.getFullYear(), m0: from.getMonth() });
      }
    }
    this.open.set(next);
  }

  protected shiftMonth(delta: number): void {
    this.view.update(({ y, m0 }) => {
      const date = new Date(y, m0 + delta, 1);
      return { y: date.getFullYear(), m0: date.getMonth() };
    });
  }

  protected pick(iso: string): void {
    const from = this.draftFrom();
    const to = this.draftTo();
    if (!from || to) {
      this.draftFrom.set(iso);
      this.draftTo.set(null);
      return;
    }
    const first = iso < from ? iso : from;
    const second = iso < from ? from : iso;
    this.rangeChange.emit({ from: first, to: second });
    this.open.set(false);
  }

  protected resetToCurrentMonth(): void {
    const now = new Date();
    const first = toIso(new Date(now.getFullYear(), now.getMonth(), 1));
    this.rangeChange.emit({ from: first, to: this.todayIso });
    this.open.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.el.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.open.set(false);
  }
}
