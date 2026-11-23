import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  NgZone,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  viewChild,
} from '@angular/core';
import {
  Chart,
  CategoryScale,
  Filler,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';

export type LinePoint = {
  label: string;
  value: number;
};

Chart.register(
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Filler,
  Tooltip,
);

const AXIS = '#64748b';
const GRID = '#dfeaf0';
const LINE = '#0d9995';
const FILL = 'rgba(13, 153, 149, 0.12)';

/**
 * Grafica de linea de escaneos basada en chart.js (tooltip incluido). Mantiene la API
 * `[points]` para poder sustituirla por otra implementacion sin tocar las pantallas.
 */
@Component({
  selector: 'app-line-chart',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="chart-wrap">
      <canvas #canvas></canvas>
      @if (!hasData()) {
        <p class="chart-empty">Aun no hay escaneos este mes.</p>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .chart-wrap {
      position: relative;
      height: 100%;
      min-height: 240px;
    }

    .chart-empty {
      position: absolute;
      inset: 0;
      display: grid;
      place-items: center;
      margin: 0;
      color: var(--color-text-muted);
    }
  `,
})
export class LineChartComponent {
  readonly points = input<LinePoint[]>([]);

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private readonly zone = inject(NgZone);
  private chart: Chart | null = null;

  constructor() {
    afterNextRender(() => {
      this.zone.runOutsideAngular(() => {
        this.chart = new Chart(this.canvas().nativeElement, {
          type: 'line',
          data: { labels: [], datasets: [dataset()] },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
              legend: { display: false },
              tooltip: { displayColors: false },
            },
            scales: {
              x: {
                grid: { display: false },
                ticks: { color: AXIS, maxTicksLimit: 6, maxRotation: 0 },
              },
              y: {
                beginAtZero: true,
                grid: { color: GRID },
                border: { display: false },
                ticks: { color: AXIS, precision: 0, maxTicksLimit: 5 },
              },
            },
          },
        });
      });
      this.applyPoints(this.points());
    });

    effect(() => {
      // leer SIEMPRE la senal: si se lee dentro del if, el effect no registra
      // dependencias y nunca se re-ejecuta cuando llegan los datos
      const points = this.points();
      if (this.chart) {
        this.zone.runOutsideAngular(() => this.applyPoints(points));
      }
    });
  }

  private applyPoints(points: LinePoint[]): void {
    const data = this.chart?.data;
    if (!this.chart || !data) {
      return;
    }
    data.labels = points.map((point) => point.label);
    data.datasets[0].data = points.map((point) => point.value);
    this.chart.update();
  }

  protected readonly hasData = computed(() => this.points().some((point) => point.value > 0));
}

function dataset() {
  return {
    label: 'Escaneos',
    data: [] as number[],
    borderColor: LINE,
    backgroundColor: FILL,
    fill: true,
    tension: 0.35,
    borderWidth: 2.5,
    pointRadius: 2,
    pointHoverRadius: 5,
    pointBackgroundColor: LINE,
  };
}
