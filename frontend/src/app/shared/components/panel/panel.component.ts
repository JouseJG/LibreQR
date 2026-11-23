import { ChangeDetectionStrategy, Component, booleanAttribute, input } from '@angular/core';

/**
 * Tarjeta contenedora. Con `fill` se convierte en columna flexible para que un hijo
 * marcado con `flex` en la pantalla ocupe el alto restante del panel.
 */
@Component({
  selector: 'app-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="panel" [class.panel--fill]="fill()">
      <ng-content select="[panel-header]" />
      <ng-content />
    </article>
  `,
  styles: `
    @use 'tokens' as *;

    :host {
      display: block;
    }

    .panel {
      background: $color-surface;
      border: 1px solid $color-border;
      border-radius: $radius-lg;
      padding: 1.25rem 1.4rem;
      // box-shadow: $shadow-soft;
    }

    .panel--fill {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 1rem;

      h2 {
        margin: 0;
        font-size: 1.05rem;
      }
    }
  `,
})
export class PanelComponent {
  readonly fill = input(false, { transform: booleanAttribute });
}
