import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-state-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="state-card">
      <div class="state-icon" [attr.aria-hidden]="true">{{ icon() }}</div>
      @if (eyebrow()) {
        <p class="eyebrow">{{ eyebrow() }}</p>
      }
      <h1>{{ title() }}</h1>
      @if (message()) {
        <p>{{ message() }}</p>
      }
      <div class="actions">
        <ng-content />
      </div>
    </div>
  `,
  styles: `
    @use 'tokens' as *;

    .state-card {
      background: $color-surface;
      border: 1px solid $color-border;
      border-radius: $radius-lg;
      // box-shadow: $shadow-soft;
      padding: 2.5rem 2rem;
      text-align: center;
      max-width: 520px;
      margin: 2rem auto;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      align-items: center;
    }

    .state-icon {
      width: 52px;
      height: 52px;
      border-radius: 50%;
      display: grid;
      place-items: center;
      font-size: 1.4rem;
      font-weight: 800;
      background: $color-primary-soft;
      color: $color-primary;
    }

    .eyebrow {
      margin: 0;
      color: $color-text-muted;
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    h1 {
      margin: 0;
      font-size: 1.5rem;
    }

    p {
      margin: 0;
      color: $color-text-muted;
    }

    .actions {
      display: flex;
      gap: 0.75rem;
      margin-top: 1rem;
    }
  `,
})
export class StateCardComponent {
  readonly icon = input('✓');
  readonly eyebrow = input('');
  readonly title = input.required<string>();
  readonly message = input('');
}
