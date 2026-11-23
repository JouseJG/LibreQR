import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CampaignApiService, CampaignSort } from '../../../../core/services/campaign-api.service';
import { problemMessage } from '../../../../core/models/api.models';
import { ListQueryState } from '../../../../core/utils/list-query-state';
import { FilterOption, ListFiltersComponent } from '../../../../shared/components/list-filters/list-filters.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { TableCardComponent } from '../../../../shared/components/table-card/table-card.component';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
import { UiTabsComponent } from '../../../../shared/components/ui-tabs/ui-tabs.component';
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
    UiTabsComponent,
    ListFiltersComponent,
    TableCardComponent,
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

      <app-list-filters
        searchLabel="Buscar"
        searchPlaceholder="Nombre o descripción"
        [search]="list.search()"
        (searchChange)="list.setSearch($event)"
        [sortOptions]="sortOptions"
        [sortBy]="list.sort()"
        (sortByChange)="list.setSort($event)">
        <ui-tabs
          listTabs
          ariaLabel="Filtrar campañas por estado"
          [tabs]="statusTabs"
          [activeKey]="list.statusTab()"
          (tabChange)="list.setStatus($event)"></ui-tabs>
      </app-list-filters>

      <app-table-card
        [loading]="list.loading()"
        [error]="list.error()"
        [empty]="campaigns().length === 0"
        emptyText="No se han encontrado campañas con estos filtros."
        [totalItems]="totalElements()"
        [page]="list.page()"
        [pageSize]="pageSize"
        (retry)="list.retry()"
        (pageChange)="list.setPage($event)">
        <table>
          <thead>
            <tr>
              <th>Campaña</th>
              <th>Descripción</th>
              <th>Estado</th>
              <th>Creación</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (campaign of campaigns(); track campaign.id) {
              <tr class="row-click" [routerLink]="['/campaigns', campaign.id]">
                <td>
                  <div class="row-name">
                    <span class="dot" style="background: #0d9995"></span>
                    {{ campaign.name }}
                  </div>
                </td>
                <td class="campaign-description">
                  {{ campaign.description || '—' }}
                </td>
                <td>
                  <status-badge
                    [status]="CAMPAIGN_STATUS_LABELS[campaign.status]"
                    [tone]="campaignStatusTone(campaign.status)"></status-badge>
                </td>
                <td>{{ campaign.createdAt | date: 'dd MMM yyyy' }}</td>
                <td class="table-actions">
                  <a class="table-link" [routerLink]="['/campaigns', campaign.id]">
                    Ver detalles
                    <span aria-hidden="true">→</span>
                  </a>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </app-table-card>
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
