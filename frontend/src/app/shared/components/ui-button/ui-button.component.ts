import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'ui-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <button
      [type]="type()"
      class="ui-button"
      [ngClass]="[
        'ui-button--' + variant(),
        'ui-button--' + size()
      ]"
      [disabled]="disabled()"
      (click)="clicked.emit($event)">
      <ng-content />
    </button>
  `,
  styles: `
    @use 'tokens' as *;

    .ui-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      border-radius: 8px;
      font-weight: 700;
      cursor: pointer;
    }

    .ui-button:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }

    .ui-button--primary {
      background: $color-primary;
      color: white;
      border: none;
     // // box-shadow: $shadow-button;
    }

    .ui-button--primary:hover:not(:disabled) {
      background: $color-primary-hover;
    }

    .ui-button--secondary {
      background: #fff;
      color: $color-primary;
      border: 1px solid rgba(13, 153, 149, 0.3);
    }

    .ui-button--secondary:hover:not(:disabled) {
      background: $color-primary-soft;
    }

    .ui-button--ghost {
      background: transparent;
      color: #475569;
      border: 1px solid $color-border;
    }

    .ui-button--danger {
      background: transparent;
      color: $color-danger;
      border: 1px solid $color-danger;
    }

    .ui-button--danger:hover:not(:disabled) {
      background: rgba(220, 38, 38, 0.08);
    }

    .ui-button--sm {
      padding: 0.6rem 0.8rem;
      font-size: 0.78rem;
    }

    .ui-button--md {
      padding: 0.78rem 1rem;
      font-size: 0.84rem;
    }
  `,
})
export class UiButtonComponent {
  readonly variant = input<'primary' | 'secondary' | 'ghost' | 'danger'>('primary');
  readonly size = input<'sm' | 'md'>('md');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly disabled = input(false);
  readonly clicked = output<MouseEvent>();
}
