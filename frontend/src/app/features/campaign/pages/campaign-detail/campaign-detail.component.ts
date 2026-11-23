import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CampaignApiService } from '../../../../core/services/campaign-api.service';
import { CampaignMetrics, problemMessage } from '../../../../core/models/api.models';
import { NotFoundComponent } from '../../../../shared/components/not-found/not-found.component';
import { PanelComponent } from '../../../../shared/components/panel/panel.component';
import { StatusBadgeComponent } from '../../../../shared/components/status-badge/status-badge.component';
import { QrThumbComponent } from '../../../../shared/components/qr-thumb/qr-thumb.component';
import { TableCardComponent } from '../../../../shared/components/table-card/table-card.component';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
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

@Component({
  selector: 'app-campaign-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    UiButtonComponent,
    PanelComponent,
    NotFoundComponent,
    StatusBadgeComponent,
    TableCardComponent,
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

        @if (metrics(); as data) {
          <app-panel>
            <div panel-header class="panel-header">
              <h2>Métricas <small>(últimos 30 días)</small></h2>
            </div>
            <dl class="campaign-metrics">
              <div>
                <dt>Escaneos humanos</dt>
                <dd>{{ data.humanScans }}</dd>
              </div>
              <div>
                <dt>Escaneos totales</dt>
                <dd>{{ data.totalScans }}</dd>
              </div>
              <div>
                <dt>Visitantes únicos</dt>
                <dd>{{ data.uniqueVisitors }}</dd>
              </div>
              <div>
                <dt>QRs activos con escaneos</dt>
                <dd>{{ data.activeWithScansPercent | number: '1.0-0' }}%</dd>
              </div>
            </dl>
          </app-panel>
        }

        <app-panel>
          <div panel-header class="panel-header">
            <h2>QRs de la campaña</h2>
            <a class="secondary-link" routerLink="/qr/new">+ Crear QR</a>
          </div>

          <app-table-card
            [loading]="qrsLoading()"
            [empty]="linkedQrs().length === 0"
            emptyText="Esta campaña todavía no tiene códigos QR asociados.">
            <table>
              <thead>
                <tr>
                  <th>Código QR</th>
                  <th>Tipo</th>
                  <th>Estado</th>
                  <th>Creación</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                @for (qr of linkedQrs(); track qr.id) {
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
                    <td>{{ QR_TYPE_LABELS[qr.type] }}</td>
                    <td>
                      <status-badge
                        [status]="QR_STATUS_LABELS[qr.status]"
                        [tone]="qrStatusTone(qr.status)"></status-badge>
                    </td>
                    <td>{{ qr.createdAt | date: 'dd MMM yyyy' }}</td>
                    <td class="table-actions">
                      <button
                        type="button"
                        class="danger-link"
                        (click)="$event.stopPropagation(); unlinkQr(qr.id)">
                        Quitar
                      </button>
                      <a class="table-link" [routerLink]="['/qr', qr.id]">Ver</a>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </app-table-card>
        </app-panel>
      }
    </section>
  `,
  styleUrl: './campaign-detail.component.scss',
})
export class CampaignDetailComponent {
  private readonly campaignApi = inject(CampaignApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly QR_TYPE_LABELS = QR_TYPE_LABELS;
  protected readonly QR_STATUS_LABELS = QR_STATUS_LABELS;
  protected readonly qrStatusTone = qrStatusTone;
  protected readonly qrShareContent = qrShareContent;
  protected readonly designFromConfig = designFromConfig;
  protected readonly CAMPAIGN_STATUS_LABELS = CAMPAIGN_STATUS_LABELS;
  protected readonly campaignStatusTone = campaignStatusTone;

  private readonly id = this.route.snapshot.paramMap.get('id') ?? '';

  protected readonly campaign = signal<Campaign | null>(null);
  protected readonly linkedQrs = signal<QrItem[]>([]);
  protected readonly qrsLoading = signal(true);
  protected readonly metrics = signal<CampaignMetrics | null>(null);
  protected readonly notFound = signal(false);
  protected readonly error = signal('');
  protected readonly confirmDelete = signal(false);

  constructor() {
    this.load();
  }

  private load(): void {
    this.campaignApi.get(this.id).subscribe({
      next: (campaign) => {
        this.campaign.set(campaign);
        this.reloadQrs();
      },
      error: () => this.notFound.set(true),
    });
  }

  protected unlinkQr(qrId: string): void {
    this.error.set('');
    this.campaignApi.removeQr(this.id, qrId).subscribe({
      next: () => this.reloadQrs(),
      error: (err) =>
        this.error.set(problemMessage(err, 'No se pudo quitar el QR de la campaña')),
    });
  }

  private reloadQrs(): void {
    this.qrsLoading.set(true);
    this.campaignApi.linkedQrs(this.id).subscribe({
      next: (qrs) => {
        this.linkedQrs.set(qrs);
        this.qrsLoading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los QRs de la campaña');
        this.qrsLoading.set(false);
      },
    });
    this.campaignApi.metrics(this.id).subscribe({
      next: (metrics) => this.metrics.set(metrics),
      error: () => this.metrics.set(null),
    });
  }

  protected changeStatus(status: CampaignStatus): void {
    this.error.set('');
    this.campaignApi.changeStatus(this.id, status).subscribe({
      next: (campaign) => {
        this.campaign.set(campaign);
        // cascado simetrico: pausar/finalizar desactiva los QRs; reactivar los reactiva
        this.reloadQrs();
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
