import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export type UiTab = {
  key: string;
  label: string;
};

/** Grupo de pestanas accesible (role=tablist) para filtros de estado de listas. */
@Component({
  selector: 'ui-tabs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="ui-tabs" role="tablist" [attr.aria-label]="ariaLabel()">
      @for (tab of tabs(); track tab.key) {
        <button
          type="button"
          role="tab"
          class="ui-tab"
          [class.ui-tab--active]="activeKey() === tab.key"
          [attr.aria-selected]="activeKey() === tab.key"
          (click)="tabChange.emit(tab.key)">
          {{ tab.label }}
        </button>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .ui-tabs {
      display: flex;
      gap: 1.25rem;
      flex-wrap: wrap;
    }

    .ui-tab {
      background: none;
      border: none;
      border-bottom: 2px solid transparent;
      padding: 0.45rem 0.1rem;
      color: var(--color-text-muted);
      font-size: 0.88rem;
      font-weight: 600;
      cursor: pointer;
    }

    .ui-tab:hover {
      color: var(--color-text);
    }

    .ui-tab--active {
      color: var(--color-primary);
      border-bottom-color: var(--color-primary);
    }
  `,
})
export class UiTabsComponent {
  readonly ariaLabel = input('');
  readonly tabs = input<UiTab[]>([]);
  readonly activeKey = input('all');
  readonly tabChange = output<string>();
}
