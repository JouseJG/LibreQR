import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { BarChartComponent, BarItem } from '../bar-chart/bar-chart.component';
import { DateRange, DateRangeComponent } from '../date-range/date-range.component';
import { DonutChartComponent, DonutSlice } from '../donut-chart/donut-chart.component';
import { LineChartComponent, LinePoint } from '../line-chart/line-chart.component';
import { MetricCardComponent } from '../metric-card/metric-card.component';
import { PanelComponent } from '../panel/panel.component';

export type StatCardVm = {
  label: string;
  value: string | number;
  subtitle?: string;
};

export type StatsPreset = {
  value: string;
  label: string;
};

/**
 * Vista de estadisticas de un perfil (campana o QR): barra de rango con presets y
 * export, tarjetas de metricas, tendencia diaria a todo el ancho y fila de
 * dispositivos (dona) + ubicacion (barras). La pagina calcula el view-model y
 * escucha `rangeChange`/`presetChange`/`exportClick` para recargar/descargar.
 */
@Component({
  selector: 'app-stats-overview',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    DateRangeComponent,
    MetricCardComponent,
    LineChartComponent,
    DonutChartComponent,
    BarChartComponent,
    PanelComponent,
  ],
  template: `
    <div class="stats-toolbar">
      <div class="stats-toolbar-left">
        <app-date-range [range]="range()" (rangeChange)="rangeChange.emit($event)" />
        <span class="stats-dot" aria-hidden="true">·</span>
        <select
          class="range-preset"
          aria-label="Periodo rapido"
          (change)="presetChange.emit($any($event.target).value)">
          @for (option of presets(); track option.value) {
            <option [value]="option.value" [selected]="preset() === option.value">
              {{ option.label }}
            </option>
          }
        </select>
      </div>
      <button type="button" class="export-btn" (click)="exportClick.emit()">
        <span aria-hidden="true">&darr;</span> Exportar datos
      </button>
    </div>

    <div class="metric-cards">
      @for (card of cards(); track card.label) {
        <metric-card
          [label]="card.label"
          [value]="card.value"
          [subtitle]="card.subtitle"></metric-card>
      }
    </div>

    <app-panel class="stats-trend" fill>
      <div panel-header class="panel-header">
        <div>
          <h2>Tendencia de escaneos</h2>
          <p class="panel-sub">Escaneos diarios · {{ rangeLabel() }}</p>
        </div>
        <span class="legend"><span class="legend-dot" aria-hidden="true"></span>Escaneos</span>
      </div>
      <div class="chart-total">{{ formatNumber(totalScans()) }}</div>
      <app-line-chart [points]="points()"></app-line-chart>
    </app-panel>

    <div class="stats-grid">
      <app-panel fill>
        <div panel-header class="panel-header">
          <div>
            <h2>Dispositivos</h2>
            <p class="panel-sub">Desde donde te escanean</p>
          </div>
        </div>
        <app-donut-chart [items]="devices()" ariaLabel="Escaneos por dispositivo"></app-donut-chart>
      </app-panel>

      <app-panel fill>
        <div panel-header class="panel-header">
          <div>
            <h2>Ubicacion de los escaneos</h2>
            <p class="panel-sub">Pais · ubicacion aproximada por idioma del navegador</p>
          </div>
        </div>
        @if (locations().length) {
          <app-bar-chart [items]="locations()"></app-bar-chart>
        } @else {
          <p class="panel-empty">Sin datos de ubicacion en el rango.</p>
        }
      </app-panel>
    </div>
  `,
  styles: `
    @use 'tokens' as *;

    :host {
      display: block;
    }

    .stats-toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      margin: 0 0 1.25rem;
    }

    .stats-toolbar-left {
      display: flex;
      align-items: center;
      gap: 0.35rem;
    }

    .stats-dot {
      color: $color-text-muted;
    }

    .range-preset {
      border: none;
      background: none;
      font: inherit;
      font-size: 0.9rem;
      font-weight: 500;
      color: $color-text-muted;
      cursor: pointer;
      padding: 4px;
      border-radius: 8px;

      &:hover {
        background: $color-primary-soft;
      }
    }

    .export-btn {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      border: 1px solid $color-border;
      background: $color-surface;
      color: $color-text;
      border-radius: 10px;
      padding: 6px 14px;
      font: inherit;
      font-size: 0.85rem;
      font-weight: 600;
      cursor: pointer;

      &:hover {
        border-color: $color-primary;
        color: $color-primary;
      }
    }

    .metric-cards {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 1.25rem;
      margin-bottom: 1.5rem;
    }

    .stats-trend {
      display: block;
      margin-bottom: 1.5rem;

      app-line-chart {
        flex: 1;
        min-height: 260px;
        display: block;
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.25rem;
      align-items: stretch;

      app-panel {
        min-width: 0;
      }
    }

    .panel-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 1rem;

      h2 {
        margin: 0;
        font-size: 1.05rem;
      }
    }

    .panel-sub {
      margin: 0.15rem 0 0;
      color: $color-text-muted;
      font-size: 0.82rem;
    }

    .panel-empty {
      color: $color-text-muted;
      text-align: center;
      padding: 1.5rem 0;
      margin: 0;
    }

    .legend {
      display: inline-flex;
      align-items: center;
      gap: 0.45rem;
      color: $color-text-muted;
      font-size: 0.82rem;
      font-weight: 600;
      white-space: nowrap;
    }

    .legend-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: $color-primary;
    }

    .chart-total {
      font-size: 1.5rem;
      font-weight: 800;
      letter-spacing: -0.03em;
      margin-bottom: 0.5rem;
    }

    @media (max-width: 1100px) {
      .metric-cards {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (max-width: 720px) {
      .metric-cards,
      .stats-grid {
        grid-template-columns: 1fr;
      }

      .stats-toolbar {
        flex-wrap: wrap;
      }
    }
  `,
})
export class StatsOverviewComponent {
  readonly range = input.required<DateRange>();
  readonly preset = input('30');
  readonly presets = input<StatsPreset[]>([]);
  readonly rangeLabel = input('');
  readonly cards = input<StatCardVm[]>([]);
  readonly points = input<LinePoint[]>([]);
  readonly devices = input<DonutSlice[]>([]);
  readonly locations = input<BarItem[]>([]);

  readonly rangeChange = output<DateRange>();
  readonly presetChange = output<string>();
  readonly exportClick = output<void>();

  protected readonly totalScans = computed(() =>
    this.points().reduce((sum, point) => sum + point.value, 0),
  );

  protected formatNumber(value: number): string {
    return new Intl.NumberFormat('es-ES').format(value);
  }
}
