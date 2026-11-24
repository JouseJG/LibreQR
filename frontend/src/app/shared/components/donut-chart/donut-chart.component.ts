import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type DonutSlice = {
  label: string;
  value: number;
};

/**
 * Grafico de dona en SVG puro (sin librerias): segmentos proporcionales al valor,
 * etiqueta central con la categoria dominante y leyenda a la derecha. Reutilizable
 * para cualquier despiece categorical (dispositivos, origen, etc.).
 */
@Component({
  selector: 'app-donut-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div class="donut" [class.donut--empty]="total() === 0">
      @if (total() > 0) {
        <svg viewBox="0 0 42 42" class="donut-svg" role="img" [attr.aria-label]="ariaLabel()">
          <circle class="donut-track" cx="21" cy="21" r="15.915" fill="none" />
          @for (slice of slices(); track slice.label) {
            <circle
              class="donut-seg"
              cx="21"
              cy="21"
              r="15.915"
              fill="none"
              [attr.stroke]="slice.color"
              [attr.stroke-dasharray]="slice.dash"
              [attr.stroke-dashoffset]="slice.offset" />
          }
          <text class="donut-center__value" x="21" y="20.5" text-anchor="middle">
            {{ centerPercent() }}
          </text>
          <text class="donut-center__label" x="21" y="25" text-anchor="middle">
            {{ centerLabel() }}
          </text>
        </svg>
        <ul class="donut-legend">
          @for (slice of slices(); track slice.label) {
            <li>
              <span class="dot" [style.background]="slice.color" aria-hidden="true"></span>
              <span class="donut-legend__name">{{ slice.label }}</span>
              <strong>{{ slice.percent | number: '1.0-0' }}%</strong>
            </li>
          }
        </ul>
      } @else {
        <p class="donut-empty">Sin datos de dispositivo en el rango.</p>
      }
    </div>
  `,
  styles: `
    @use 'tokens' as *;

    :host {
      display: block;
    }

    .donut {
      display: flex;
      align-items: center;
      gap: 1.25rem;
    }

    .donut-svg {
      width: 128px;
      height: 128px;
      flex-shrink: 0;
    }

    .donut-track {
      stroke: $color-surface-alt;
      stroke-width: 4.2;
    }

    .donut-seg {
      stroke-width: 4.2;
    }

    .donut-center__value {
      font-size: 7px;
      font-weight: 800;
      fill: $color-text;
    }

    .donut-center__label {
      font-size: 3px;
      font-weight: 600;
      fill: $color-text-muted;
    }

    .donut-legend {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      font-size: 0.85rem;
      min-width: 0;

      li {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      strong {
        margin-left: auto;
        font-variant-numeric: tabular-nums;
      }
    }

    .donut-legend__name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    .donut-empty {
      color: $color-text-muted;
      font-size: 0.85rem;
      margin: 0;
    }
  `,
})
export class DonutChartComponent {
  readonly items = input<DonutSlice[]>([]);
  readonly ariaLabel = input('Despiece por categoria');

  private static readonly COLORS = [
    '#0d9995', // primary
    '#4f46e5', // purple
    '#d97706', // warning
    '#1ea672', // success
    '#64748b', // muted
  ];

  readonly total = computed(() =>
    this.items().reduce((sum, item) => sum + item.value, 0),
  );

  /** Segmentos con geometria (pathLength=100) + porcentaje y color resueltos. */
  readonly slices = computed(() => {
    const total = this.total();
    if (total === 0) {
      return [];
    }
    let cumulative = 0;
    return this.items()
      .filter((item) => item.value > 0)
      .sort((a, b) => b.value - a.value)
      .map((item, index) => {
        const percent = (item.value / total) * 100;
        const dash = `${percent} ${100 - percent}`;
        const offset = 100 - cumulative + 25; // 25 = inicio en las 12 en punto
        cumulative += percent;
        return {
          label: item.label,
          value: item.value,
          percent,
          color: DonutChartComponent.COLORS[index % DonutChartComponent.COLORS.length],
          dash,
          offset,
        };
      });
  });

  readonly centerPercent = computed(() => {
    const top = this.slices()[0];
    return top ? `${Math.round(top.percent)}%` : '';
  });

  readonly centerLabel = computed(() => this.slices()[0]?.label ?? '');
}
