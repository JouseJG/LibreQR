import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CampaignApiService } from '../../../../core/services/campaign-api.service';
import { QrApiService, QrSort } from '../../../../core/services/qr-api.service';
import { problemMessage } from '../../../../core/models/api.models';
import { ListQueryState } from '../../../../core/utils/list-query-state';
import { FilterOption, ListFiltersComponent } from '../../../../shared/components/list-filters/list-filters.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { QrThumbComponent } from '../../../../shared/components/qr-thumb/qr-thumb.component';
import { TableCardComponent } from '../../../../shared/components/table-card/table-card.component';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
import { UiTabsComponent } from '../../../../shared/components/ui-tabs/ui-tabs.component';
import { Campaign } from '../../../campaign/models/campaign.model';
import { designFromConfig } from '../../models/qr-design';
import {
  QrItem,
  QrPage,
  QrStatus,
  qrShareContent,
  QR_STATUS_LABELS,
  QR_TYPE_LABELS,
  qrStatusTone,
} from '../../models/qr.model';

const PAGE_SIZE = 5;

@Component({
  selector: 'app-qr-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    UiButtonComponent,
    UiTabsComponent,
    ListFiltersComponent,
    TableCardComponent,
    StatusBadgeComponent,
    QrThumbComponent,
  ],
  template: `
    <section class="page-shell">
      <header class="page-header">
        <div>
          <h1>Códigos QR</h1>
        </div>
        <ui-button variant="primary" routerLink="/qr/new">+ Crear QR</ui-button>
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
          ariaLabel="Filtrar códigos QR por estado"
          [tabs]="statusTabs"
          [activeKey]="list.statusTab()"
          (tabChange)="list.setStatus($event)"></ui-tabs>
      </app-list-filters>

      <app-table-card
        [loading]="list.loading()"
        [error]="list.error()"
        [empty]="qrCodes().length === 0"
        emptyText="No se han encontrado códigos QR con estos filtros."
        [totalItems]="totalElements()"
        [page]="list.page()"
        [pageSize]="pageSize"
        (retry)="list.retry()"
        (pageChange)="list.setPage($event)">
        <table>
          <thead>
            <tr>
              <th>Código QR</th>
              <th>Campaña</th>
              <th>Tipo</th>
              <th>Estado</th>
              <th>Escaneos</th>
              <th>Creación</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (qr of qrCodes(); track qr.id) {
              <tr class="row-click" [routerLink]="['/qr', qr.id]">
                <td>
                  <div class="row-name">
                    <qr-thumb
                      [content]="qrShareContent(qr)"
                      [design]="designFromConfig(qr.designConfig)"
                      [size]="40"></qr-thumb>
                    {{ qr.name }}
                  </div>
                </td>
                <td>{{ campaignName(qr.campaignId) }}</td>
                <td>{{ QR_TYPE_LABELS[qr.type] }}</td>
                <td>
                  <status-badge
                    [status]="QR_STATUS_LABELS[qr.status]"
                    [tone]="qrStatusTone(qr.status)"></status-badge>
                </td>
                <td>{{ scanCounts()[qr.id] ?? 0 }}</td>
                <td>{{ qr.createdAt | date: 'dd MMM yyyy' }}</td>
                <td class="table-actions">
                  <a class="table-link" [routerLink]="['/qr', qr.id]">
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
})
export class QrListComponent {
  private readonly qrApi = inject(QrApiService);
  private readonly campaignApi = inject(CampaignApiService);

  protected readonly pageSize = PAGE_SIZE;
  protected readonly QR_STATUS_LABELS = QR_STATUS_LABELS;
  protected readonly QR_TYPE_LABELS = QR_TYPE_LABELS;
  protected readonly qrStatusTone = qrStatusTone;
  protected readonly qrShareContent = qrShareContent;
  protected readonly designFromConfig = designFromConfig;

  protected readonly sortOptions: FilterOption[] = [
    { value: 'createdAt,desc', label: 'Más reciente' },
    { value: 'createdAt,asc', label: 'Más antiguo' },
    { value: 'name,asc', label: 'Nombre A-Z' },
    { value: 'name,desc', label: 'Nombre Z-A' },
  ];

  protected readonly statusTabs = [
    { key: 'all', label: 'Todos' },
    { key: 'active', label: 'Activos' },
    { key: 'inactive', label: 'Inactivos' },
  ];

  protected readonly list = new ListQueryState({ reload: () => this.load() });
  protected readonly data = signal<QrPage | null>(null);
  protected readonly scanCounts = signal<Record<string, number>>({});
  protected readonly campaignNames = signal<Record<string, string>>({});

  protected readonly qrCodes = computed<QrItem[]>(() => this.data()?.content ?? []);
  protected readonly totalElements = computed(() => this.data()?.totalElements ?? 0);

  constructor() {
    this.load();
    this.campaignApi.list(0, 100, 'name,asc').subscribe({
      next: (page) => {
        const map: Record<string, string> = {};
        for (const campaign of page.content) {
          map[campaign.id] = campaign.name;
        }
        this.campaignNames.set(map);
      },
      error: () => undefined,
    });
  }

  protected load(): void {
    this.list.loading.set(true);
    this.list.error.set('');
    this.qrApi
      .list(
        this.list.page() - 1,
        PAGE_SIZE,
        this.list.sort() as QrSort,
        this.list.searchParam(),
        this.list.statusParam<QrStatus>(),
      )
      .subscribe({
        next: (page) => {
          this.data.set(page);
          this.list.loading.set(false);
          this.loadScanCounts(page.content.map((qr) => qr.id));
        },
        error: (err) => {
          this.list.error.set(problemMessage(err, 'No se pudieron cargar los códigos QR'));
          this.list.loading.set(false);
        },
      });
  }

  protected campaignName(campaignId: string | null): string {
    return (campaignId && this.campaignNames()[campaignId]) || '—';
  }

  private loadScanCounts(ids: string[]): void {
    this.scanCounts.set({});
    this.qrApi.scanCounts(ids).subscribe({
      next: (counts) => this.scanCounts.set(counts),
      error: () => this.scanCounts.set({}),
    });
  }
}
