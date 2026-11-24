import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ColorFieldComponent } from '../../../../shared/components/color-field/color-field.component';
import { QrThumbComponent } from '../../../../shared/components/qr-thumb/qr-thumb.component';
import { UiTabsComponent } from '../../../../shared/components/ui-tabs/ui-tabs.component';
import {
  CORNER_DOT_OPTIONS,
  CORNER_OPTIONS,
  DOT_OPTIONS,
  QrCornerDotType,
  QrCornerType,
  QrDesign,
  QrDotType,
  QrLogoSource,
} from '../../models/qr-design';

const MAX_LOGO_BYTES = 300 * 1024;

const LOGO_OPTIONS_BASE: { value: QrLogoSource; label: string }[] = [
  { value: 'none', label: 'Sin logo' },
  { value: 'brand', label: 'Logo LibreQR' },
];

/**
 * Disenador de QR (forma y logo) reutilizado por la creacion y la edicion. Las
 * opciones se previsualizan con los iconos SVG de `public/assets/svg`. El marco de
 * texto quedo fuera del menu (los disenos antiguos lo conservan al renderizar).
 * Uso: <app-qr-designer [(design)]="design" [content]="textoPreview" />
 */
@Component({
  selector: 'app-qr-designer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, UiTabsComponent, QrThumbComponent, ColorFieldComponent],
  template: `
    <ui-tabs
      ariaLabel="Seccion de diseno del QR"
      [tabs]="designTabs"
      [activeKey]="tab()"
      (tabChange)="tab.set($event)"></ui-tabs>

    @switch (tab()) {
      @case('dot') {
        <div class="design-group">
          <h3>Trama de puntos</h3>
          <div class="option-row">
            @for (option of dotOptions; track option.value) {
              <button
                type="button"
                class="design-option"
                [class.design-option--active]="design().dotType === option.value"
                [attr.aria-pressed]="design().dotType === option.value"
                [attr.aria-label]="option.label"
                [title]="option.label"
                (click)="setDot(option.value)">
                <img class="design-icon" [src]="option.icon" alt="" width="36" height="36" />
              </button>
            }
          </div>
          <div class="color-row">
            <app-color-field
              label="Color de la trama"
              [value]="design().dotColor"
              (change)="setColor('dotColor', $event)"></app-color-field>
            <app-color-field
              label="Color de fondo"
              [value]="design().background"
              (change)="setColor('background', $event)"></app-color-field>
          </div>

          <h3>Esquinas</h3>
          <div class="option-row">
            @for (option of cornerOptions; track option.value) {
              <button
                type="button"
                class="design-option"
                [class.design-option--active]="design().cornerType === option.value"
                [attr.aria-pressed]="design().cornerType === option.value"
                [attr.aria-label]="option.label"
                [title]="option.label"
                (click)="setCorner(option.value)">
                <img class="design-icon" [src]="option.icon" alt="" width="36" height="36" />
              </button>
            }
          </div>
          <app-color-field
            label="Color de las esquinas"
            [value]="design().cornerColor"
            (change)="setColor('cornerColor', $event)"></app-color-field>

          <h3>Centro de las esquinas</h3>
          <div class="option-row">
            @for (option of cornerDotOptions; track option.value) {
              <button
                type="button"
                class="design-option"
                [class.design-option--active]="design().cornerDotType === option.value"
                [attr.aria-pressed]="design().cornerDotType === option.value"
                [attr.aria-label]="option.label"
                [title]="option.label"
                (click)="setCornerDot(option.value)">
                <img class="design-icon" [src]="option.icon" alt="" width="36" height="36" />
              </button>
            }
          </div>
          <app-color-field
            label="Color del centro"
            [value]="design().cornerDotColor"
            (change)="setColor('cornerDotColor', $event)"></app-color-field>
        </div>
      }
      @case('logo') {
        <div class="design-group">
          <label class="upload">
            <span>Subir logo (PNG/JPG/SVG/WebP, máx. 300 KB)</span>
            <input type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml" (change)="onLogoFile($event)" />
          </label>
          @if (logoError()) {
            <p class="form-error" role="alert">{{ logoError() }}</p>
          }
          <div class="option-row">
            @for (option of logoOptions(); track option.value) {
              <button
                type="button"
                class="design-option"
                [class.design-option--active]="design().logo === option.value"
                [attr.aria-pressed]="design().logo === option.value"
                (click)="setLogo(option.value)">
                <qr-thumb [content]="previewContent()" [design]="option.design" [size]="44"></qr-thumb>
                <span>{{ option.label }}</span>
              </button>
            }
          </div>
        </div>
      }
    }
  `,
  styles: `
    @use 'tokens' as *;

    :host {
      display: block;
    }

    .design-group {
      margin-top: 1rem;

      h3 {
        margin: 0 0 0.6rem;
        font-size: 0.82rem;
        color: $color-text-muted;
        text-transform: uppercase;
        letter-spacing: 0.05em;

        &:not(:first-child) {
          margin-top: 1.1rem;
        }
      }
    }

    .design-field {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
      font-size: 0.88rem;
      margin: 0.9rem 0;

      label {
        color: $color-text-muted;
        font-weight: 600;
      }
    }

    .option-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.6rem;
    }

    .design-option {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.35rem;
      background: $color-surface-alt;
      border: 1px solid $color-border;
      border-radius: $radius-md;
      padding: 0.55rem 0.7rem;
      font-size: 0.74rem;
      font-weight: 600;
      color: $color-text-muted;
      cursor: pointer;
      min-width: 76px;

      &:hover {
        border-color: $color-primary;
      }

      &--active {
        border-color: $color-primary;
        background: $color-primary-soft;
        color: $color-primary-hover;
      }
    }

    .design-icon {
      object-fit: contain;
      color: $color-text;
    }

    .color-row {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      margin-top: 0.9rem;
    }

    .upload {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
      font-size: 0.82rem;
      color: $color-text-muted;
      font-weight: 600;
      margin-bottom: 1rem;

      input[type='file'] {
        font-size: 0.84rem;
      }
    }

    .form-error {
      color: $color-danger;
      font-size: 0.85rem;
    }
  `,
})
export class QrDesignerComponent {
  readonly design = input.required<QrDesign>();
  readonly content = input('');
  readonly designChange = output<QrDesign>();

  protected readonly tab = signal('dot');
  protected readonly logoError = signal('');

  protected readonly designTabs = [
    { key: 'dot', label: 'Forma' },
    { key: 'logo', label: 'Logo' },
  ];

  protected readonly previewContent = computed(() => this.content());

  protected readonly dotOptions = DOT_OPTIONS;
  protected readonly cornerOptions = CORNER_OPTIONS;
  protected readonly cornerDotOptions = CORNER_DOT_OPTIONS;

  protected readonly logoOptions = computed(() => {
    const base =
      this.design().logoData.length > 0
        ? [{ value: 'custom' as QrLogoSource, label: 'Logo subido' }, ...LOGO_OPTIONS_BASE]
        : LOGO_OPTIONS_BASE;
    return base.map((option) => ({
      ...option,
      design: { ...this.design(), logo: option.value },
    }));
  });

  protected setDot(dotType: QrDotType): void {
    this.apply({ dotType });
  }

  protected setCorner(cornerType: QrCornerType): void {
    this.apply({ cornerType });
  }

  protected setCornerDot(cornerDotType: QrCornerDotType): void {
    this.apply({ cornerDotType });
  }

  protected setLogo(logo: QrLogoSource): void {
    this.apply({ logo });
  }

  protected setColor(
    key: 'frameTextColor' | 'dotColor' | 'background' | 'cornerColor' | 'cornerDotColor',
    value: string,
  ): void {
    if (!/^#[0-9a-fA-F]{6}$/.test(value.trim())) {
      return;
    }
    this.apply({ [key]: value.trim() });
  }

  protected onLogoFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';    if (!file) {
      return Promise.resolve();
    }
    if (file.size > MAX_LOGO_BYTES) {
      this.logoError.set('El logo supera los 300 KB. Usa una imagen más ligera.');
      return Promise.resolve();
    }
    this.logoError.set('');
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => {
        if (typeof reader.result === 'string') {
          this.apply({ logo: 'custom', logoData: reader.result as string });
        }
        resolve();
      };
      reader.onerror = () => {
        this.logoError.set('No se pudo leer la imagen.');
        resolve();
      };
      reader.readAsDataURL(file);
    });
  }

  private apply(patch: Partial<QrDesign>): void {
    this.designChange.emit({ ...this.design(), ...patch });
  }
}
