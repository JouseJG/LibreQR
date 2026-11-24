import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { QrApiService } from '../../../../core/services/qr-api.service';
import { QrStats } from '../../../../core/models/api.models';
import {
  StatsRangeState,
  statsDailyAverage,
  statsDeviceSlices,
  statsLocationItems,
  statsNumber,
  statsPercentOfAll,
  statsTrendPoints,
} from '../../../../core/utils/stats-view';
import { StatCardVm, StatsOverviewComponent } from '../../../../shared/components/stats-overview/stats-overview.component';
import { MetricCardComponent } from '../../../../shared/components/metric-card/metric-card.component';
import { UiTabsComponent } from '../../../../shared/components/ui-tabs/ui-tabs.component';
import { CampaignApiService } from '../../../../core/services/campaign-api.service';
import { QrThumbComponent } from '../../../../shared/components/qr-thumb/qr-thumb.component';
import { designFromConfig } from '../../models/qr-design';
import { problemMessage } from '../../../../core/models/api.models';
import { NotFoundComponent } from '../../../../shared/components/not-found/not-found.component';
import { PanelComponent } from '../../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
import {
  QrItem,
  QrMetricsResponse,
  QR_STATUS_LABELS,
  QR_TYPE_LABELS,
  qrShareContent,
  qrStatusTone,
} from '../../models/qr.model';

@Component({
  selector: 'app-qr-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    UiButtonComponent,
    PanelComponent,
    NotFoundComponent,
    StatusBadgeComponent,
    QrThumbComponent,
    UiTabsComponent,
    StatsOverviewComponent,
    MetricCardComponent,
  ],
  template: `
    <section class="page-shell">
      <a class="back-link" routerLink="/qr">← Volver a códigos QR</a>

      @if (notFound()) {
        <app-not-found message="Este código QR ya no existe o fue eliminado."></app-not-found>
      } @else if (qr(); as selectedQr) {
        <header class="page-header">
          <div>
            <h1>{{ selectedQr.name }}</h1>
            <p class="qr-subtitle">
              {{ QR_TYPE_LABELS[selectedQr.type] }} ·
              <status-badge
                [status]="QR_STATUS_LABELS[selectedQr.status]"
                [tone]="qrStatusTone(selectedQr.status)"></status-badge>
            </p>
          </div>
          <div class="qr-detail-actions">
            @if (confirmDelete()) {
              <ui-button variant="danger" size="sm" (click)="remove()">Confirmar eliminación</ui-button>
              <ui-button variant="ghost" size="sm" (click)="confirmDelete.set(false)">Cancelar</ui-button>
            } @else {
              <ui-button variant="danger" size="sm" (click)="confirmDelete.set(true)">
                Eliminar
              </ui-button>
            }
          </div>
        </header>

        @if (error()) {
          <p class="form-error" role="alert">{{ error() }}</p>
        }

        <ui-tabs
          class="qr-tabs"
          ariaLabel="Secciones del codigo QR"
          [tabs]="sectionTabs"
          [activeKey]="activeTab()"
          (tabChange)="activeTab.set($event)"></ui-tabs>

        @switch (activeTab()) {
          @case ('info') {
            <div class="info-grid">
              <div class="info-col">
                <app-panel>
                  <div panel-header class="panel-header">
                    <h2>Destino y campaña</h2>
                    <a class="inline-link" [routerLink]="['/qr', selectedQr.id, 'edit']">
                      Editar <span aria-hidden="true">→</span>
                    </a>
                  </div>

                  <p class="field-label">{{ selectedQr.type === 'url' ? 'URL de destino' : 'Contenido' }}</p>
                  <p class="field-value qr-payload">{{ selectedQr.payload }}</p>
                  @if (selectedQr.description) {
                    <p class="field-description">{{ selectedQr.description }}</p>
                  }

                  @if (selectedQr.slug) {
                    <p class="field-label">Enlace corto de seguimiento</p>
                    <div class="field-row">
                      <code class="field-value">{{ shortLink(selectedQr) }}</code>
                      <button type="button" class="copy-btn" (click)="copyShortLink(selectedQr)">
                        {{ copied() ? '¡Copiado!' : 'Copiar' }}
                      </button>
                    </div>
                  }

                  <div class="two-col">
                    <div>
                      <p class="field-label">Campaña asociada</p>
                      @if (selectedQr.campaignId && campaignName(); as name) {
                        <a class="inline-link" [routerLink]="['/campaigns', selectedQr.campaignId]">
                          {{ name }} <span aria-hidden="true">→</span>
                        </a>
                      } @else {
                        <p class="field-value">—</p>
                      }
                    </div>
                    <div>
                      <p class="field-label">Fecha de creación</p>
                      <p class="field-value">{{ selectedQr.createdAt | date: 'd MMM yyyy' }}</p>
                    </div>
                  </div>
                </app-panel>

                <div class="quick-stats">
                  <metric-card
                    [label]="'Escaneos en ' + monthLabel"
                    [value]="monthMetrics() ? statsNumber(monthMetrics()!.humanScans) : '—'"
                    [trendText]="scansTrendText()"
                    [tone]="scansTrendTone()"></metric-card>
                  <metric-card
                    label="Visitantes únicos"
                    [value]="monthMetrics() ? statsNumber(monthMetrics()!.uniqueVisitors) : '—'"
                    subtitle="En lo que va de mes"></metric-card>
                </div>

                <app-panel>
                  <div panel-header class="panel-header">
                    <h2>Estado del código</h2>
                  </div>
                  <div class="status-row">
                    <div>
                      <p class="status-title">
                        {{ selectedQr.status === 'active' ? 'Tu código está activo' : 'Tu código está inactivo' }}
                      </p>
                      <p class="status-desc">
                        {{ selectedQr.status === 'active'
                          ? 'Redirige a tu destino y registra los escaneos.'
                          : 'No redirige hasta que lo reactives; los enlaces dejarán de funcionar.' }}
                      </p>
                    </div>
                    <div class="status-actions">
                      @if (selectedQr.status === 'active') {
                        @if (confirmDeactivate()) {
                          <ui-button variant="secondary" size="sm" (click)="deactivate()">
                            Confirmar desactivación
                          </ui-button>
                          <ui-button variant="ghost" size="sm" (click)="confirmDeactivate.set(false)">
                            Cancelar
                          </ui-button>
                        } @else {
                          <ui-button variant="secondary" size="sm" (click)="confirmDeactivate.set(true)">
                            <span aria-hidden="true">⏸</span> Pausar código
                          </ui-button>
                        }
                      } @else {
                        <ui-button variant="secondary" size="sm" (click)="reactivate()">
                          <span aria-hidden="true">▶</span> Reactivar código
                        </ui-button>
                      }
                    </div>
                  </div>
                </app-panel>
              </div>

              <app-panel class="code-panel">
                <div panel-header class="panel-header">
                  <div>
                    <h2>Tu código QR</h2>
                    <p class="panel-sub">
                      Últimos 30 días · {{ qrStats() ? statsNumber(qrStats()!.scans) : '—' }}
                    </p>
                  </div>
                </div>

                <div class="qr-big">
                  <qr-thumb
                    #thumb
                    [content]="qrShareContent(selectedQr)"
                    [design]="designFromConfig(selectedQr.designConfig)"
                    [size]="220"></qr-thumb>
                </div>
                <p class="qr-big-caption">
                  {{ selectedQr.campaignId && campaignName() ? 'QR de ' + campaignName() : 'QR de tu cuenta' }}
                </p>

                <div class="download-btns">
                  <ui-button variant="primary" size="sm" (click)="downloadPng()">
                    <span aria-hidden="true">↓</span> Descargar PNG
                  </ui-button>
                  <ui-button variant="secondary" size="sm" (click)="downloadSvg()">
                    <span aria-hidden="true">↓</span> Descargar SVG
                  </ui-button>
                </div>

                <p class="print-note">
                  <span aria-hidden="true">◉</span> Usa SVG para imprimir en alta calidad.
                </p>
              </app-panel>
            </div>
          }
          @case ('stats') {
            @if (qrStats(); as data) {
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
  styleUrl: './qr-detail.component.scss',
})
export class QrDetailComponent {
  private readonly qrApi = inject(QrApiService);
  private readonly campaignApi = inject(CampaignApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly QR_TYPE_LABELS = QR_TYPE_LABELS;
  protected readonly QR_STATUS_LABELS = QR_STATUS_LABELS;
  protected readonly qrStatusTone = qrStatusTone;
  protected readonly qrShareContent = qrShareContent;
  protected readonly designFromConfig = designFromConfig;
  protected readonly statsNumber = statsNumber;

  private readonly id = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('id') ?? '' },
  );

  protected readonly qr = signal<QrItem | null>(null);
  protected readonly qrStats = signal<QrStats | null>(null);
  protected readonly activeTab = signal('info');
  protected readonly sectionTabs = [
    { key: 'info', label: 'Informacion del codigo' },
    { key: 'stats', label: 'Estadisticas' },
  ];
  protected readonly statsRange = new StatsRangeState(() => this.loadStats());
  protected readonly campaignName = signal('');
  protected readonly notFound = signal(false);
  protected readonly error = signal('');
  protected readonly copied = signal(false);
  protected readonly confirmDeactivate = signal(false);
  protected readonly confirmDelete = signal(false);

  /** Tarjetas resumidas del mes en curso para la pestana de informacion. */
  protected readonly monthMetrics = signal<QrMetricsResponse | null>(null);
  protected readonly monthLabel = new Intl.DateTimeFormat('es-ES', { month: 'long' })
    .format(new Date())
    .replace(/^\w/, (c) => c.toUpperCase());
  private readonly thumb = viewChild<QrThumbComponent>('thumb');

  constructor() {
    this.load();
  }

  private load(): void {
    this.qrApi.get(this.id()).subscribe({
      next: (qr) => {
        this.qr.set(qr);
        this.loadStats();
        this.loadMonthMetrics();
        if (qr.campaignId) {
          this.campaignApi.get(qr.campaignId).subscribe({
            next: (campaign) => this.campaignName.set(campaign.name),
            error: () => this.campaignName.set(''),
          });
        }
      },
      error: () => this.notFound.set(true),
    });
  }

  private loadStats(): void {
    const range = this.statsRange.range();
    this.qrApi.stats(this.id(), range.from, range.to).subscribe({
      next: (stats) => this.qrStats.set(stats),
      error: () => this.qrStats.set(null),
    });
  }

  private loadMonthMetrics(): void {
    const now = new Date();
    const from = new Date(Date.UTC(now.getFullYear(), now.getMonth(), 1)).toISOString();
    this.qrApi.metrics(this.id(), from, now.toISOString()).subscribe({
      next: (metrics) => this.monthMetrics.set(metrics),
      error: () => this.monthMetrics.set(null),
    });
  }

  protected scansTrendText(): string | undefined {
    const data = this.monthMetrics();
    if (!data) {
      return undefined;
    }
    const change =
      data.previousPeriodScans === 0
        ? data.humanScans > 0
          ? 100
          : 0
        : ((data.humanScans - data.previousPeriodScans) / data.previousPeriodScans) * 100;
    const formatted = new Intl.NumberFormat('es-ES', {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(Math.abs(change));
    return `${change >= 0 ? '↑' : '↓'} ${formatted}% vs. mes anterior`;
  }

  protected scansTrendTone(): 'success' | 'down' | 'default' {
    const data = this.monthMetrics();
    if (!data || data.humanScans === data.previousPeriodScans) {
      return 'default';
    }
    return data.humanScans > data.previousPeriodScans ? 'success' : 'down';
  }

  protected downloadPng(): void {
    this.thumb()?.download(this.qr()?.name ?? 'libreqr', 'png');
  }

  protected downloadSvg(): void {
    this.thumb()?.download(this.qr()?.name ?? 'libreqr', 'svg');
  }

  protected statCards(data: QrStats): StatCardVm[] {
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
        label: 'Bots detectados',
        value: statsNumber(data.botScans),
        subtitle: 'Descartados del recuento principal',
      },
    ];
  }

  protected readonly trendPoints = computed(() =>
    statsTrendPoints(this.qrStats()?.dailySeries ?? []),
  );
  protected readonly deviceSlices = computed(() =>
    statsDeviceSlices(this.qrStats()?.deviceTypes ?? []),
  );
  protected readonly locationItems = computed(() =>
    statsLocationItems(this.qrStats()?.countries ?? [], this.qrStats()?.scans ?? 0),
  );

  protected exportStats(): void {
    const range = this.statsRange.range();
    const link = document.createElement('a');
    link.href = this.qrApi.statsExportUrl(this.id(), range.from, range.to);
    link.download = '';
    link.click();
  }

  protected shortLink(qr: QrItem): string {
    return `${location.origin}/r/${qr.slug}`;
  }

  protected async copyShortLink(qr: QrItem): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.shortLink(qr));
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.copied.set(false);
    }
  }
  protected goToEdit(): void {
    const qr = this.qr();
    if (qr) {
      void this.router.navigate(['/qr', qr.id, 'edit']);
    }
  }

  protected deactivate(): void {
    const qr = this.qr();
    if (!qr) {
      return;
    }
    this.qrApi.deactivate(qr.id).subscribe({
      next: (updated) => {
        this.qr.set(updated);
        this.confirmDeactivate.set(false);
      },
      error: (err) =>
        this.error.set(problemMessage(err, 'No se pudo desactivar el QR')),
    });
  }

  protected reactivate(): void {
    const qr = this.qr();
    if (!qr) {
      return;
    }
    this.error.set('');
    this.qrApi.reactivate(qr.id).subscribe({
      next: (updated) => this.qr.set(updated),
      error: (err) =>
        this.error.set(problemMessage(err, 'No se pudo reactivar el QR')),
    });
  }

  protected remove(): void {
    const qr = this.qr();
    if (!qr) {
      return;
    }
    this.qrApi.delete(qr.id).subscribe({
      next: () => this.router.navigate(['/qr']),
      error: (err) => this.error.set(problemMessage(err, 'No se pudo eliminar el QR')),
    });
  }
}
