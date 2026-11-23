import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Tarjeta de resumen: etiqueta, numero y un pie de linea obligatorio (comparativa o
 * matiz) para que todas las cards del dashboard mantengan la misma altura.
 */
@Component({
  selector: 'metric-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="metric-card">
      <div class="metric-card__label">{{ label() }}</div>
      <div class="metric-card__row">
        <strong>{{ value() }}</strong>
      </div>
      <div class="metric-card__foot" [class]="footClass()">
        {{ footText() }}&nbsp;
      </div>
    </article>
  `,
  styles: `
    @use 'tokens' as *;

    :host {
      display: block;
      height: 100%;
    }

    .metric-card {
      background: $color-surface;
      border: 1px solid $color-border;
      border-radius: $radius-md;
      padding: 1rem 1.1rem;
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .metric-card__label {
      color: $color-text-muted;
      font-size: 0.82rem;
    }

    .metric-card__row {
      display: flex;
      align-items: center;
      margin-top: 0.8rem;
    }

    .metric-card__row strong {
      font-size: 1.8rem;
      letter-spacing: -0.04em;
    }

    .metric-card__foot {
      margin-top: 0.45rem;
      font-size: 0.78rem;
      font-weight: 700;
      min-height: 1.15em;
    }

    .metric-card__foot--muted {
      color: $color-text-muted;
      font-weight: 600;
    }

    .metric-card__foot--up {
      color: $color-success;
    }

    .metric-card__foot--down {
      color: $color-danger;
    }
  `,
})
export class MetricCardComponent {
  readonly label = input('');
  readonly value = input<string | number>('');
  /** Comparativa ya formateada (p. ej. "+18,6% vs mayo"); se pinta segun `tone`. */
  readonly trendText = input<string | undefined>(undefined);
  /** Texto gris discreto bajo el numero (p. ej. "12 en total"). */
  readonly subtitle = input<string | undefined>(undefined);
  readonly tone = input<'success' | 'down' | 'default'>('default');

  protected readonly footText = computed(() => this.trendText() ?? this.subtitle() ?? '');

  protected readonly footClass = computed(() => {
    if (this.trendText()) {
      return this.tone() === 'success'
        ? 'metric-card__foot--up'
        : this.tone() === 'down'
          ? 'metric-card__foot--down'
          : 'metric-card__foot--muted';
    }
    return 'metric-card__foot--muted';
  });
}
