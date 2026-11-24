import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CampaignApiService, CampaignSort } from '../../../../core/services/campaign-api.service';
import { problemMessage } from '../../../../core/models/api.models';
import { ListQueryState } from '../../../../core/utils/list-query-state';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { FilterOption } from '../../../../shared/components/list-filters/list-filters.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
import {
  Campaign,
  CampaignPage,
  CampaignStatus,
  CAMPAIGN_STATUS_LABELS,
  campaignStatusTone,
} from '../../models/campaign.model';

const PAGE_SIZE = 5;

@Component({
  selector: 'app-campaign-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    UiButtonComponent,
    DataTableComponent,
    StatusBadgeComponent,
  ],
  template: `
    <section class="page-shell">
      <header class="page-header">
        <div>
          <h1>Campañas</h1>
        </div>
        <ui-button variant="primary" routerLink="/campaigns/new">+ Crear campaña</ui-button>
      </header>

      <app-data-table
        [query]="list"
        [rows]="campaigns()"
        [columns]="['Campaña', 'Descripción', 'Estado', 'Creación', '']"
        [total]="totalElements()"
        [pageSize]="pageSize"
        searchPlaceholder="Nombre o descripción"
        emptyText="No se han encontrado campañas con estos filtros."
        [sortOptions]="sortOptions"
        [tabs]="statusTabs"
        tabsAriaLabel="Filtrar campañas por estado"
        [rowLink]="campaignRowLink">
        <ng-template #rowCells let-campaign>
          <td>
            <div class="row-name">
              <span class="dot" style="background: #0d9995"></span>
              {{ $any(campaign).name }}
            </div>
          </td>
          <td class="campaign-description">
            {{ $any(campaign).description || '—' }}
          </td>
          <td>
            <status-badge
              [status]="statusLabel(campaign)"
              [tone]="campaignStatusTone($any(campaign).status)"></status-badge>
          </td>
          <td>{{ $any(campaign).createdAt | date: 'dd MMM yyyy' }}</td>
          <td class="table-actions">
            <a class="table-link" [routerLink]="['/campaigns', $any(campaign).id]">
              Ver detalles
              <span aria-hidden="true">→</span>
            </a>
          </td>
        </ng-template>
      </app-data-table>
    </section>
  `,
  styles: `
    .campaign-description {
      color: var(--color-text-muted);
      max-width: 340px;
      overflow-wrap: anywhere;
    }
  `,
})
export class CampaignListComponent {
  private readonly campaignApi = inject(CampaignApiService);

  protected readonly pageSize = PAGE_SIZE;
  protected readonly CAMPAIGN_STATUS_LABELS = CAMPAIGN_STATUS_LABELS;
  protected readonly campaignStatusTone = campaignStatusTone;

  protected readonly sortOptions: FilterOption[] = [
    { value: 'createdAt,desc', label: 'Más reciente' },
    { value: 'createdAt,asc', label: 'Más antigua' },
    { value: 'name,asc', label: 'Nombre A-Z' },
    { value: 'name,desc', label: 'Nombre Z-A' },
  ];

  protected readonly statusTabs = [
    { key: 'all', label: 'Todas' },
    { key: 'active', label: 'Activas' },
    { key: 'paused', label: 'Pausadas' },
    { key: 'finished', label: 'Finalizadas' },
  ];

  protected statusLabel(campaign: unknown): string {
    return CAMPAIGN_STATUS_LABELS[(campaign as Campaign).status];
  }

  protected readonly campaignRowLink = (campaign: Campaign): unknown[] => [
    '/campaigns',
    campaign.id,
  ];

  protected readonly list = new ListQueryState({ reload: () => this.load() });
  protected readonly data = signal<CampaignPage | null>(null);

  protected readonly campaigns = computed<Campaign[]>(() => this.data()?.content ?? []);
  protected readonly totalElements = computed(() => this.data()?.totalElements ?? 0);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.list.loading.set(true);
    this.list.error.set('');
    this.campaignApi
      .list(
        this.list.page() - 1,
        PAGE_SIZE,
        this.list.sort() as CampaignSort,
        this.list.searchParam(),
        this.list.statusParam<CampaignStatus>(),
      )
      .subscribe({
        next: (page) => {
          this.data.set(page);
          this.list.loading.set(false);
        },
        error: (err) => {
          this.list.error.set(problemMessage(err, 'No se pudieron cargar las campañas'));
          this.list.loading.set(false);
        },
      });
  }
}
