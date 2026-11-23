import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CampaignScan, DailyScanPoint, StatsSummary } from '../../../../core/models/api.models';
import { CampaignApiService } from '../../../../core/services/campaign-api.service';
import { QrApiService } from '../../../../core/services/qr-api.service';
import { StatsApiService } from '../../../../core/services/stats-api.service';
import { LineChartComponent, LinePoint } from '../../../../shared/components/line-chart/line-chart.component';
import { BarChartComponent, BarItem } from '../../../../shared/components/bar-chart/bar-chart.component';
import { MetricCardComponent } from '../../../../shared/components/metric-card/metric-card.component';
import { PanelComponent } from '../../../../shared/components/panel/panel.component';
import { QrThumbComponent } from '../../../../shared/components/qr-thumb/qr-thumb.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
import { Campaign, CAMPAIGN_STATUS_LABELS } from '../../../campaign/models/campaign.model';
import { designFromConfig } from '../../../qr/models/qr-design';
import {
  QR_STATUS_LABELS,
  QR_TYPE_LABELS,
  QrItem,
  qrShareContent,
  qrStatusTone,
} from '../../../qr/models/qr.model';

const RECENT_CAMPAIGNS = 4;
const RECENT_QRS = 5;

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    MetricCardComponent,
    LineChartComponent,
    BarChartComponent,
    PanelComponent,
    StatusBadgeComponent,
    QrThumbComponent,
    UiButtonComponent,
  ],
  template: `
    <section class="page-shell">
      <header class="page-header">
        <div>
          <h1>Dashboard</h1>
          <p class="page-sub">Esta es la actividad de tus campañas y códigos QR.</p>
        </div>
        <div class="dashboard-actions">
          <ui-button variant="secondary" routerLink="/campaigns/new">Nueva campaña</ui-button>
          <ui-button variant="primary" routerLink="/qr/new">+ Crear QR</ui-button>
        </div>
      </header>

      <div class="metric-cards">
        <metric-card
          label="Escaneos totales"
          [value]="stats()?.scansCurrentMonth ?? '—'"
          [trendText]="monthComparison(stats()?.scansCurrentMonth, stats()?.scansPreviousMonth)"
          [tone]="monthTone(stats()?.scansCurrentMonth, stats()?.scansPreviousMonth)"></metric-card>
        <metric-card
          label="Visitantes únicos"
          [value]="stats()?.uniqueCurrentMonth ?? '—'"
          [trendText]="monthComparison(stats()?.uniqueCurrentMonth, stats()?.uniquePreviousMonth)"
          [tone]="monthTone(stats()?.uniqueCurrentMonth, stats()?.uniquePreviousMonth)"></metric-card>
        <metric-card
          label="Códigos QR activos"
          [value]="stats()?.activeQrs ?? '—'"
          [subtitle]="totalQrsSubtitle()"></metric-card>
        <metric-card
          label="Campañas activas"
          [value]="stats()?.activeCampaigns ?? '—'"
          [subtitle]="totalCampaignsSubtitle()"></metric-card>
      </div>

      <div class="charts-grid">
        <app-panel fill>
          <div panel-header class="panel-header">
            <div>
              <h2>Tendencia de escaneos</h2>
              <p class="panel-sub">Escaneos diarios · {{ monthLabel }}</p>
            </div>
            <span class="legend"><span class="legend-dot" aria-hidden="true"></span>Escaneos</span>
          </div>

          <div class="chart-total">{{ formatNumber(totalMonthScans()) }}</div>
          <app-line-chart [points]="trendPoints()"></app-line-chart>
        </app-panel>

        <app-panel fill>
          <div panel-header class="panel-header">
            <div>
              <h2>Campañas destacadas</h2>
              <p class="panel-sub">Por volumen de escaneos</p>
            </div>
            <a class="inline-link" routerLink="/campaigns">Ver todas <span aria-hidden="true">→</span></a>
          </div>

          @if (topCampaignScans().length) {
            <app-bar-chart
              selectable
              [items]="barItems()"
              (itemClick)="openCampaign($event)"></app-bar-chart>
            @if (leaderName(); as leader) {
              <div class="campaign-foot">
                <p class="campaign-foot__title">«{{ leader }}» lidera la actividad.</p>
                <p class="campaign-foot__sub">Concentra el {{ leaderPercent() }}% de los escaneos.</p>
              </div>
            }
          } @else {
            <p class="panel-empty">Aún no hay escaneos este mes.</p>
          }
        </app-panel>
      </div>

      <div class="dashboard-grid">
        <app-panel>
          <div panel-header class="panel-header">
            <h2>QRs recientes</h2>
            <a class="inline-link" routerLink="/qr">Ver todos <span aria-hidden="true">→</span></a>
          </div>

          <div class="mini-table">
            <table>
              <tbody>
                @for (qr of recentQrs(); track qr.id) {
                  <tr class="row-click" [routerLink]="['/qr', qr.id]">
                    <td>
                      <div class="row-name">
                        <qr-thumb
                          [content]="qrShareContent(qr)"
                          [design]="designFromConfig(qr.designConfig)"
                          [size]="34"></qr-thumb>
                        {{ qr.name }}
                        <small>{{ QR_TYPE_LABELS[qr.type] }}</small>
                      </div>
                    </td>
                    <td>
                      <status-badge
                        [status]="QR_STATUS_LABELS[qr.status]"
                        [tone]="qrStatusTone(qr.status)"></status-badge>
                    </td>
                    <td class="row-date">{{ qr.createdAt | date: 'dd MMM' }}</td>
                    <td class="table-actions">
                      <a class="table-link" [routerLink]="['/qr', qr.id]">
                        Ver <span aria-hidden="true">→</span>
                      </a>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td class="empty-state">
                      Sin QRs todavía. <a class="inline-link" routerLink="/qr/new">Crea el primero</a>.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </app-panel>

        <app-panel>
          <div panel-header class="panel-header">
            <h2>Campañas recientes</h2>
            <a class="inline-link" routerLink="/campaigns">Ver todas <span aria-hidden="true">→</span></a>
          </div>

          <div class="mini-table">
            <table>
              <tbody>
                @for (campaign of recentCampaigns(); track campaign.id) {
                  <tr class="row-click" [routerLink]="['/campaigns', campaign.id]">
                    <td>
                      <div class="row-name">
                        <span class="dot" [ngClass]="'dot--' + campaign.status"></span>
                        {{ campaign.name }}
                        <small>{{ CAMPAIGN_STATUS_LABELS[campaign.status] }}</small>
                      </div>
                    </td>
                    <td class="row-date">{{ campaign.createdAt | date: 'dd MMM' }}</td>
                    <td class="table-actions">
                      <a class="table-link" [routerLink]="['/campaigns', campaign.id]">
                        Ver <span aria-hidden="true">→</span>
                      </a>
                    </td>
                  </tr>
                } @empty {
                  <tr>
                    <td class="empty-state">
                      Sin campañas. <a class="inline-link" routerLink="/campaigns/new">Crea la primera</a>.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </app-panel>
      </div>
    </section>
  `,
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly campaignApi = inject(CampaignApiService);
  private readonly qrApi = inject(QrApiService);
  private readonly statsApi = inject(StatsApiService);
  private readonly router = inject(Router);

  protected readonly QR_TYPE_LABELS = QR_TYPE_LABELS;
  protected readonly QR_STATUS_LABELS = QR_STATUS_LABELS;
  protected readonly CAMPAIGN_STATUS_LABELS = CAMPAIGN_STATUS_LABELS;
  protected readonly qrStatusTone = qrStatusTone;
  protected readonly qrShareContent = qrShareContent;
  protected readonly designFromConfig = designFromConfig;

  protected readonly stats = signal<StatsSummary | null>(null);
  protected readonly dailyScans = signal<DailyScanPoint[]>([]);
  protected readonly campaignScans = signal<CampaignScan[]>([]);
  protected readonly recentCampaigns = signal<Campaign[]>([]);
  protected readonly recentQrs = signal<QrItem[]>([]);

  /** Nombre del mes en curso ("junio 2026") para la grafica de tendencia. */
  protected readonly monthLabel = new Intl.DateTimeFormat('es-ES', {
    month: 'long',
    year: 'numeric',
  }).format(new Date());

  /** Nombre del mes anterior en castellano, para la comparativa ("vs mayo"). */
  private readonly previousMonthName = new Intl.DateTimeFormat('es-ES', { month: 'long' })
    .format(new Date(new Date().getFullYear(), new Date().getMonth() - 1, 15));

  constructor() {
    this.statsApi.summary().subscribe({
      next: (stats) => this.stats.set(stats),
      error: () => undefined,
    });
    this.statsApi.dailyScans().subscribe({
      next: (points) => this.dailyScans.set(points),
      error: () => undefined,
    });
    this.statsApi.campaignScans().subscribe({
      next: (rows) => this.campaignScans.set(rows),
      error: () => undefined,
    });
    this.campaignApi.list(0, RECENT_CAMPAIGNS).subscribe({
      next: (page) => this.recentCampaigns.set(page.content),
      error: () => undefined,
    });
    this.qrApi.list(0, RECENT_QRS).subscribe({
      next: (page) => this.recentQrs.set(page.content),
      error: () => undefined,
    });
  }

  protected readonly totalMonthScans = computed(() =>
    this.dailyScans().reduce((sum, point) => sum + point.scans, 0),
  );

  protected readonly trendPoints = computed<LinePoint[]>(() =>
    this.dailyScans().map((point) => ({
      label: new Intl.DateTimeFormat('es-ES', { day: 'numeric', month: 'short' })
        .format(new Date(`${point.date}T00:00:00Z`))
        .replace('.', ''),
      value: point.scans,
    })),
  );

  protected readonly topCampaignScans = computed(() =>
    this.campaignScans().filter((row) => row.scans > 0).slice(0, 4),
  );

  protected readonly barItems = computed<BarItem[]>(() =>
    this.topCampaignScans().map((row) => ({
      label: row.name,
      value: row.scans,
      key: row.campaignId,
    })),
  );

  protected openCampaign(item: BarItem): void {
    if (item.key) {
      void this.router.navigate(['/campaigns', item.key]);
    }
  }

  protected leaderName(): string | undefined {
    return this.topCampaignScans()[0]?.name;
  }

  protected leaderPercent(): string {
    const leader = this.topCampaignScans()[0];
    const total = this.campaignScans().reduce((sum, row) => sum + row.scans, 0);
    if (!leader || total === 0) {
      return '0';
    }
    return new Intl.NumberFormat('es-ES', {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format((leader.scans / total) * 100);
  }

  protected formatNumber(value: number): string {
    return new Intl.NumberFormat('es-ES').format(value);
  }

  protected monthComparison(current?: number, previous?: number): string | undefined {
    if (current === undefined || previous === undefined) {
      return undefined;
    }
    if (previous === 0) {
      // sin base de comparacion: se muestra como crecimiento maximo si hay actividad
      const sign = current > 0 ? '+100' : '0';
      return `${sign}% vs ${this.previousMonthName}`;
    }
    const change = ((current - previous) / previous) * 100;
    const formatted = new Intl.NumberFormat('es-ES', {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(Math.abs(change));
    return `${change >= 0 ? '+' : '-'}${formatted}% vs ${this.previousMonthName}`;
  }

  protected monthTone(current?: number, previous?: number): 'success' | 'down' | 'default' {
    if (current === undefined || previous === undefined || current === previous) {
      return 'default';
    }
    return current > previous ? 'success' : 'down';
  }

  protected totalQrsSubtitle(): string | undefined {
    const stats = this.stats();
    return stats ? `${stats.totalQrs} en total` : undefined;
  }

  protected totalCampaignsSubtitle(): string | undefined {
    const stats = this.stats();
    return stats ? `${stats.totalCampaigns} en total` : undefined;
  }
}
