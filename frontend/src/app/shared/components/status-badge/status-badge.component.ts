import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type BadgeTone = 'success' | 'warning' | 'neutral' | 'purple';

@Component({
  selector: 'status-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <span class="status-badge" [ngClass]="'status-badge--' + tone()">
      {{ status() }}
    </span>
  `,
  styles: `
    @use 'tokens' as *;

    :host {
      display: inline-flex;
    }

    .status-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0.28rem 0.58rem;
      border-radius: 999px;
      font-size: 0.7rem;
      font-weight: 700;
      line-height: 1;
      min-width: 72px;
    }

    .status-badge--success {
      background: $color-success-soft;
      color: $color-success;
    }

    .status-badge--warning {
      background: $color-warning-soft;
      color: $color-warning;
    }

    .status-badge--neutral {
      background: rgba(148, 163, 184, 0.12);
      color: #475569;
    }

    .status-badge--purple {
      background: $color-purple-soft;
      color: $color-purple;
    }
  `,
})
export class StatusBadgeComponent {
  readonly status = input('Activo');
  readonly tone = input<BadgeTone>('success');
}
