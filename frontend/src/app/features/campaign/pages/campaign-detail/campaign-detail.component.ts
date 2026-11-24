import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CampaignApiService } from '../../../../core/services/campaign-api.service';
import { CampaignStats, Page, problemMessage } from '../../../../core/models/api.models';
import { ListQueryState } from '../../../../core/utils/list-query-state';
import {
  statsDailyAverage,
  statsDeviceSlices,
  statsLocationItems,
  statsNumber,
  statsPercentOfAll,
  StatsRangeState,
  statsTrendPoints,
} from '../../../../core/utils/stats-view';
import { DataTableComponent } from '../../../../shared/components/data-table/data-table.component';
import { StatCardVm, StatsOverviewComponent } from '../../../../shared/components/stats-overview/stats-overview.component';
import { FilterOption } from '../../../../shared/components/list-filters/list-filters.component';
import { NotFoundComponent } from '../../../../shared/components/not-found/not-found.component';
import { PanelComponent } from '../../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { QrThumbComponent } from '../../../../shared/components/qr-thumb/qr-thumb.component';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
import { UiTabsComponent } from '../../../../shared/components/ui-tabs/ui-tabs.component';
import {
  Campaign,
  CampaignStatus,
  CAMPAIGN_STATUS_LABELS,
  campaignStatusTone,
} from '../../models/campaign.model';
import {
  QrItem,
  QR_STATUS_LABELS,
  QR_TYPE_LABELS,
  qrShareContent,
  qrStatusTone,
} from '../../../qr/models/qr.model';
import { designFromConfig } from '../../../qr/models/qr-design';

const QR_PAGE_SIZE = 5;

@Component({
  selector: 'app-campaign-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    UiButtonComponent,
    UiTabsComponent,
    DataTableComponent,
    StatsOverviewComponent,
    PanelComponent,
    NotFoundComponent,
    StatusBadgeComponent,
    QrThumbComponent,
  ],
  template: `
    <section class="page-shell">
      <a class="back-link" routerLink="/campaigns">← Volver a campañas</a>

      @if (notFound()) {
        <app-not-found message="Esta campaña ya no existe o fue eliminada."></app-not-found>
      } @else if (campaign(); as selectedCampaign) {
        <header class="page-header">
          <div>
            <h1>{{ selectedCampaign.name }}</h1>
            <p class="campaign-subtitle">
              <status-badge
                [status]="CAMPAIGN_STATUS_LABELS[selectedCampaign.status]"
                [tone]="campaignStatusTone(selectedCampaign.status)"></status-badge>
            </p>
            @if (selectedCampaign.description) {
              <p>{{ selectedCampaign.description }}</p>
            }
          </div>
          <div class="campaign-actions">
            @if (selectedCampaign.status === 'active') {
              <ui-button variant="secondary" size="sm" (click)="changeStatus('paused')">
                Pausar
              </ui-button>
              <ui-button variant="ghost" size="sm" (click)="changeStatus('finished')">
                Finalizar
              </ui-button>
            } @else if (selectedCampaign.status === 'paused') {
              <ui-button variant="secondary" size="sm" (click)="changeStatus('active')">
                Reactivar
              </ui-button>
              <ui-button variant="ghost" size="sm" (click)="changeStatus('finished')">
                Finalizar
              </ui-button>
            } @else {
              <ui-button variant="secondary" size="sm" (click)="changeStatus('active')">
                Reactivar
              </ui-button>
            }
            @if (confirmDelete()) {
              <ui-button variant="danger" size="sm" (click)="remove()">Confirmar eliminación</ui-button>
              <ui-button variant="ghost" size="sm" (click)="confirmDelete.set(false)">Cancelar</ui-button>
            } @else {
              <ui-button variant="danger" size="sm" (click)="confirmDelete.set(true)">
                Eliminar campaña
              </ui-button>
            }
          </div>
        </header>

        @if (error()) {
          <p class="form-error" role="alert">{{ error() }}</p>
        }

        <ui-tabs
          class="campaign-tabs"
          ariaLabel="Secciones de la campaña"
          [tabs]="sectionTabs"
          [activeKey]="activeTab()"
          (tabChange)="activeTab.set($event)"></ui-tabs>

        @switch (activeTab()) {
          @case ('qrs') {
            <app-data-table
              [query]="list"
              [rows]="linkedQrs()"
              [columns]="['Código QR', 'Tipo', 'Estado', 'Creación', '']"
              [total]="totalLinked()"
              [pageSize]="qrPageSize"
              searchPlaceholder="Nombre o descripción"
              emptyText="Esta campaña todavía no tiene códigos QR asociados."
              [sortOptions]="qrSortOptions"
              [rowLink]="qrRowLink">
              <ng-template #rowCells let-qr>
                <td>
                  <div class="row-name">
                    <qr-thumb
                      [content]="qrShareContent($any(qr))"
                      [design]="designFromConfig($any(qr).designConfig)"
                      [size]="40"></qr-thumb>
                    {{ $any(qr).name }}
                  </div>
                </td>
                <td>{{ qrTypeLabel(qr) }}</td>
                <td>
                  <status-badge
                    [status]="qrStatusLabel(qr)"
                    [tone]="qrStatusTone($any(qr).status)"></status-badge>
                </td>
                <td>{{ $any(qr).createdAt | date: 'dd MMM yyyy' }}</td>
                <td class="table-actions">
                  <button
                    type="button"
                    class="danger-link"
                    (click)="$event.stopPropagation(); unlinkQr($any(qr).id)">
                    Quitar
                  </button>
                  <a class="table-link" [routerLink]="['/qr', $any(qr).id]">Ver</a>
                </td>
              </ng-template>
            </app-data-table>
          }
          @case ('stats') {
            @if (campaignStats(); as data) {
              <app-stats-overview
                [range]="statsRange.range()"
                [preset]="statsRange.preset()"
                [presets]="statsRange.presets"
                [rangeLabel]="statsRange.label"
                [cards]="statCards(data)"
                [points]="trendPoints()"
                [devices]="deviceSlices()"
                [locations]="locationItems()"
                (rangeChange)="statsRange.onRangeChange($event)"
                (presetChange)="statsRange.applyPreset($event)"
                (exportClick)="exportStats()"></app-stats-overview>
            } @else {
              <p class="panel-empty" role="status">Cargando estadisticas…</p>
            }
          }
        }
      }
    </section>
  `,
  styleUrl: './campaign-detail.component.scss',
})
export class CampaignDetailComponent {
  private readonly campaignApi = inject(CampaignApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly QR_STATUS_LABELS = QR_STATUS_LABELS;
  protected readonly qrStatusTone = qrStatusTone;
  protected readonly qrShareContent = qrShareContent;
  protected readonly designFromConfig = designFromConfig;
  protected readonly CAMPAIGN_STATUS_LABELS = CAMPAIGN_STATUS_LABELS;
  protected readonly campaignStatusTone = campaignStatusTone;
  protected readonly qrPageSize = QR_PAGE_SIZE;

  protected readonly sectionTabs = [
    { key: 'qrs', label: 'Códigos QR asociados' },
    { key: 'stats', label: 'Estadísticas' },
  ];

  protected readonly qrSortOptions: FilterOption[] = [
    { value: 'createdAt,desc', label: 'Más reciente' },
    { value: 'createdAt,asc', label: 'Más antiguo' },
    { value: 'name,asc', label: 'Nombre A-Z' },
    { value: 'name,desc', label: 'Nombre Z-A' },
  ];

  private readonly id = this.route.snapshot.paramMap.get('id') ?? '';

  protected readonly activeTab = signal('qrs');
  protected readonly campaign = signal<Campaign | null>(null);
  protected readonly qrPage = signal<Page<QrItem> | null>(null);
  protected readonly campaignStats = signal<CampaignStats | null>(null);
  protected readonly notFound = signal(false);
  protected readonly error = signal('');
  protected readonly confirmDelete = signal(false);

  protected readonly list = new ListQueryState({
    reload: () => this.loadQrs(),
    defaultSort: 'createdAt,desc',
  });

  protected readonly linkedQrs = computed<QrItem[]>(() => this.qrPage()?.content ?? []);
  protected readonly totalLinked = computed(() => this.qrPage()?.totalElements ?? 0);

  constructor() {
    this.load();
  }

  private load(): void {
    this.campaignApi.get(this.id).subscribe({
      next: (campaign) => {
        this.campaign.set(campaign);
        this.list.loading.set(true);
        this.loadQrs();
        this.loadStats();
      },
      error: () => this.notFound.set(true),
    });
  }

  protected qrTypeLabel(qr: unknown): string {
    return QR_TYPE_LABELS[(qr as QrItem).type];
  }

  protected qrStatusLabel(qr: unknown): string {
    return QR_STATUS_LABELS[(qr as QrItem).status];
  }

  protected readonly qrRowLink = (qr: QrItem): unknown[] => ['/qr', qr.id];

  protected unlinkQr(qrId: string): void {
    this.error.set('');
    this.campaignApi.removeQr(this.id, qrId).subscribe({
      next: () => this.list.retry(),
      error: (err) =>
        this.error.set(problemMessage(err, 'No se pudo quitar el QR de la campaña')),
    });
  }

  private loadQrs(): void {
    this.list.loading.set(true);
    this.campaignApi
      .linkedQrsPage(
        this.id,
        this.list.page() - 1,
        QR_PAGE_SIZE,
        this.list.sort(),
        this.list.searchParam(),
      )
      .subscribe({
        next: (page) => {
          this.qrPage.set(page);
          this.list.loading.set(false);
        },
        error: (err) => {
          this.list.error.set(
            problemMessage(err, 'No se pudieron cargar los QRs de la campaña'),
          );
          this.list.loading.set(false);
        },
      });
  }

  private loadStats(): void {
    const range = this.statsRange.range();
    this.campaignApi.stats(this.id, range.from, range.to).subscribe({
      next: (stats) => this.campaignStats.set(stats),
      error: () => this.campaignStats.set(null),
    });
  }

  // --- Estadisticas: rango compartido (StatsRangeState) y view-model ---

  protected readonly statsRange = new StatsRangeState(() => this.loadStats());

  protected readonly trendPoints = computed(() =>
    statsTrendPoints(this.campaignStats()?.dailySeries ?? []),
  );
  protected readonly deviceSlices = computed(() =>
    statsDeviceSlices(this.campaignStats()?.deviceTypes ?? []),
  );
  protected readonly locationItems = computed(() =>
    statsLocationItems(this.campaignStats()?.countries ?? [], this.campaignStats()?.scans ?? 0),
  );

  protected exportStats(): void {
    const range = this.statsRange.range();
    const link = document.createElement('a');
    link.href = this.campaignApi.statsExportUrl(this.id, range.from, range.to);
    link.download = '';
    link.click();
  }

  protected statCards(data: CampaignStats): StatCardVm[] {
    return [
      {
        label: 'Escaneos totales',
        value: statsNumber(data.scans),
        subtitle: statsPercentOfAll(data.scans, data.scansAllTime),
      },
      {
        label: 'Visitantes unicos',
        value: statsNumber(data.uniqueVisitors),
        subtitle: 'Personas distintas (estimacion por cookie)',
      },
      {
        label: 'Media diaria',
        value: statsNumber(statsDailyAverage(data.scans, this.statsRange.days)),
        subtitle: this.statsRange.label,
      },
      {
        label: 'Codigos QR activos',
        value: data.activeQrs,
        subtitle: `De ${data.totalQrs} codigos en la campana`,
      },
    ];
  }


  protected changeStatus(status: CampaignStatus): void {
    this.error.set('');
    this.campaignApi.changeStatus(this.id, status).subscribe({
      next: (campaign) => {
        this.campaign.set(campaign);
        // cascado simetrico: pausar/finalizar desactiva los QRs; reactivar los reactiva
        this.list.retry();
        this.loadStats();
      },
      error: (err) =>
        this.error.set(problemMessage(err, 'No se pudo cambiar el estado de la campaña')),
    });
  }

  protected remove(): void {
    this.error.set('');
    this.campaignApi.delete(this.id).subscribe({
      next: () => this.router.navigate(['/campaigns']),
      error: (err) =>
        this.error.set(problemMessage(err, 'No se pudo eliminar la campaña')),
    });
  }

}
