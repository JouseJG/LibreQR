import { ChangeDetectionStrategy, Component, computed, input, output, booleanAttribute } from '@angular/core';

export type BarItem = {
  label: string;
  value: number;
  /** Clave estable para track (por defecto se usa el indice). */
  key?: string;
};

/**
 * Grafica de barras horizontales ligera (sin librerias): nombre + valor formateado
 * y barra proporcional al maximo. Reutilizable en cualquier panel de ranking.
 * Emite `itemClick` con el item pulsado para que cada pantalla decida la accion.
 */
@Component({
  selector: 'app-bar-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ul class="bar-chart">
      @for (item of items(); track item.key ?? $index) {
        <li class="bar-row" [class.bar-row--clickable]="selectable()" (click)="emitClick(item)">
          <div class="bar-head">
            <span>{{ item.label }}</span>
            <strong>{{ format(item.value) }}</strong>
          </div>
          <div class="bar-track">
            <div class="bar-fill" [style.width.%]="width(item.value)"></div>
          </div>
        </li>
      }
    </ul>
  `,
  styles: `
    @use 'tokens' as *;

    :host {
      display: block;
    }

    .bar-chart {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .bar-head {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      font-size: 0.9rem;
      margin-bottom: 0.35rem;

      strong {
        font-variant-numeric: tabular-nums;
      }
    }

    .bar-track {
      height: 8px;
      border-radius: 999px;
      background: $color-surface-alt;
      border: 1px solid $color-border;
      overflow: hidden;
    }

    .bar-fill {
      height: 100%;
      background: $color-primary;
      border-radius: 999px;
      min-width: 4px;
    }

    .bar-row--clickable {
      cursor: pointer;
    }
  `,
})
export class BarChartComponent {
  readonly items = input<BarItem[]>([]);
  /** Maximo de la escala; por defecto el mayor valor (o 1). */
  readonly max = input<number | null>(null);
  readonly selectable = input(false, { transform: booleanAttribute });
  readonly itemClick = output<BarItem>();

  private readonly effectiveMax = computed(() =>
    this.max() ?? Math.max(1, ...this.items().map((item) => item.value)),
  );

  protected width(value: number): number {
    return (value / this.effectiveMax()) * 100;
  }

  protected format(value: number): string {
    return new Intl.NumberFormat('es-ES').format(value);
  }

  protected emitClick(item: BarItem): void {
    if (this.selectable()) {
      this.itemClick.emit(item);
    }
  }
}
