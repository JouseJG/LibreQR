import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** Selector de color (rueda + hex) para los ajustes de diseno del QR. */
@Component({
  selector: 'app-color-field',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule],
  template: `
    <label class="color-field">
      <span>{{ label() }}</span>
      <span class="color-input">
        <input
          type="color"
          [attr.aria-label]="label()"
          [disabled]="disabled()"
          [ngModel]="value()"
          [ngModelOptions]="{ standalone: true }"
          (ngModelChange)="change.emit($event)" />
        <input
          type="text"
          maxlength="7"
          [disabled]="disabled()"
          [ngModel]="value()"
          [ngModelOptions]="{ standalone: true }"
          (ngModelChange)="onText($event)" />
      </span>
    </label>
  `,
  styles: `
    @use 'tokens' as *;

    .color-field {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      font-size: 0.82rem;
      color: $color-text-muted;
      font-weight: 600;

      .color-input {
        display: flex;
        align-items: center;
        gap: 0.4rem;

        input[type='color'] {
          width: 38px;
          height: 34px;
          padding: 2px;
          border: 1px solid $color-border;
          border-radius: $radius-sm;
          background: $color-surface;
          cursor: pointer;

          &:disabled {
            opacity: 0.5;
            cursor: not-allowed;
          }
        }

        input[type='text'] {
          width: 92px;

          &:disabled {
            opacity: 0.5;
          }
        }
      }
    }
  `,
})
export class ColorFieldComponent {
  readonly label = input('');
  readonly value = input.required<string>();
  readonly disabled = input(false);
  readonly change = output<string>();

  protected onText(value: string): void {
    const color = value.trim();
    if (/^#[0-9a-fA-F]{6}$/.test(color)) {
      this.change.emit(color);
    }
  }
}
