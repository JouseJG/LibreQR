import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-not-found',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="not-found">
      <p>{{ message() }}</p>
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .not-found {
      text-align: center;
      padding: 2rem;
      color: rgba(23, 35, 47, 0.6);
    }
  `,
})
export class NotFoundComponent {
  readonly message = input('Recurso no disponible');
}
