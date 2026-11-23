import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  input,
  viewChild,
} from '@angular/core';
import QrCode from 'qr-code-styling';
import {
  DEFAULT_QR_DESIGN,
  QrDesign,
} from '../../../features/qr/models/qr-design';

const LOGO_SRC = '/assets/libreqr_logo.png';

/**
 * Miniatura SVG de un codigo QR generada en el navegador con qr-code-styling (el
 * backend aun no sirve imagenes por MinIO). Reacciona a cambios de `content`,
 * `design` y `size` repintando el SVG al vuelo. Expone `download()`.
 */
@Component({
  selector: 'qr-thumb',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<div #box class="qr-thumb" role="img" aria-label="Codigo QR"></div>`,
  styles: `
    :host {
      display: inline-flex;
      flex-shrink: 0;
    }
  `,
})
export class QrThumbComponent {
  readonly content = input.required<string>();
  readonly size = input(40);
  readonly design = input<QrDesign>(DEFAULT_QR_DESIGN);

  private readonly box = viewChild.required<ElementRef<HTMLDivElement>>('box');
  private appended = false;

  private readonly qr = new QrCode({
    type: 'svg',
    margin: 1,
    width: 40,
    height: 40,
    qrOptions: { errorCorrectionLevel: 'M' },
  });

  private readonly options = computed(() => ({
    ...optionsFor(this.design(), this.size()),
    // patron neutro mientras no hay contenido (las miniaturas de opciones se montan siempre)
    data: this.content() || 'https://libreqr.local',
  }));

  constructor() {
    afterNextRender(() => {
      if (!this.appended) {
        this.qr.update(this.options());
        this.qr.append(this.box().nativeElement);
        this.appended = true;
      }
    });

    effect(() => {
      // leer SIEMPRE las senales antes de decidir: si no, el effect no registra
      // dependencias y nunca vuelve a ejecutarse (el motivo del bug de repintado)
      const options = this.options();
      if (this.appended) {
        this.qr.update(options);
      }
    });
  }

  /** Descarga el PNG/SVG con el diseno actual (usado en la pantalla de creacion). */
  download(name = 'libreqr', extension: 'png' | 'svg' = 'png'): void {
    this.qr.download({ name, extension });
  }
}

export function optionsFor(design: QrDesign, size: number): Record<string, unknown> {
  const hasLabel = design.frame === 'text' && design.frameText.trim().length > 0;
  const dotsOptions: Record<string, unknown> = { type: design.dotType, color: design.dotColor };
  const logoSrc =
    design.logo === 'custom' && design.logoData
      ? design.logoData
      : design.logo === 'brand'
        ? LOGO_SRC
        : undefined;
  return {
    width: size,
    height: size,
    dotsOptions,
    cornersSquareOptions: { visible: true, type: design.cornerType, color: design.cornerColor },
    cornersDotOptions: { visible: true, type: design.cornerDotType, color: design.cornerDotColor },
    backgroundOptions: { visible: true, color: design.background },
    labelOptions: hasLabel
      ? {
          value: design.frameText.trim(),
          fontName: 'Readex Pro',
          color: design.frameTextColor,
          fontSize: Math.max(12, Math.round(size * 0.16)),
          margin: Math.round(size * 0.12),
        }
      : { value: undefined },
    image: logoSrc,
    logoSize: 0.3,
    logoMargin: Math.max(2, Math.round(size * 0.04)),
  } as Record<string, unknown>;
}
