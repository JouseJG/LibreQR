import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { QrApiService } from '../../../../core/services/qr-api.service';
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
  QR_STATUS_LABELS,
  QR_TYPE_LABELS,
  qrShareContent,
  qrStatusTone,
  QrMetricsResponse,
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
            <ui-button variant="secondary" size="sm" (click)="goToEdit()">Editar</ui-button>
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
                  Desactivar
                </ui-button>
              }
            } @else {
              <ui-button variant="secondary" size="sm" (click)="reactivate()">Reactivar</ui-button>
            }
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

        <div class="qr-detail-grid">
          <app-panel>
            <div panel-header class="panel-header">
              <h2>Destino</h2>
              @if (selectedQr.slug) {
                <button type="button" class="copy-btn" (click)="copyShortLink(selectedQr)">
                  {{ copied() ? '¡Copiado!' : 'Copiar enlace corto' }}
                </button>
              }
            </div>

            <div class="qr-preview">
              <qr-thumb
                [content]="qrShareContent(selectedQr)"
                [design]="designFromConfig(selectedQr.designConfig)"
                [size]="150"></qr-thumb>
            </div>

            <dl class="qr-meta">
              <div>
                <dt>Tipo</dt>
                <dd>{{ QR_TYPE_LABELS[selectedQr.type] }}</dd>
              </div>
              <div>
                <dt>Contenido</dt>
                <dd class="qr-payload">{{ selectedQr.payload }}</dd>
              </div>
              @if (selectedQr.description) {
                <div>
                  <dt>Descripción</dt>
                  <dd>{{ selectedQr.description }}</dd>
                </div>
              }
              <div>
                <dt>Campaña</dt>
                <dd>
                  @if (selectedQr.campaignId && campaignName(); as name) {
                    <a class="inline-link" [routerLink]="['/campaigns', selectedQr.campaignId]">
                      {{ name }} <span aria-hidden="true">→</span>
                    </a>
                  } @else {
                    —
                  }
                </dd>
              </div>
              @if (selectedQr.slug) {
                <div>
                  <dt>Enlace corto</dt>
                  <dd>{{ shortLink(selectedQr) }}</dd>
                </div>
              }
              <div>
                <dt>Fecha de creación</dt>
                <dd>{{ selectedQr.createdAt | date: 'dd MMM yyyy, HH:mm' }}</dd>
              </div>
              <div>
                <dt>Última actualización</dt>
                <dd>{{ selectedQr.updatedAt | date: 'dd MMM yyyy, HH:mm' }}</dd>
              </div>
            </dl>
          </app-panel>

          <app-panel>
            <div panel-header class="panel-header">
              <h2>Métricas <small>(últimos 30 días)</small></h2>
            </div>

            @if (metrics(); as data) {
              <dl class="qr-metrics">
                <div>
                  <dt>Escaneos humanos</dt>
                  <dd>{{ data.humanScans }}</dd>
                </div>
                <div>
                  <dt>Escaneos totales</dt>
                  <dd>{{ data.totalScans }}</dd>
                </div>
                <div>
                  <dt>Bots detectados</dt>
                  <dd>{{ data.botScans }}</dd>
                </div>
                <div>
                  <dt>Visitantes únicos</dt>
                  <dd>{{ data.uniqueVisitors }}</dd>
                </div>
                <div>
                  <dt>Media diaria</dt>
                  <dd>{{ data.dailyAverage | number: '1.0-1' }}</dd>
                </div>
                <div>
                  <dt>Periodo anterior</dt>
                  <dd>{{ data.previousPeriodScans }}</dd>
                </div>
                @if (data.busiestDay) {
                  <div>
                    <dt>Día con más actividad</dt>
                    <dd>
                      {{ data.busiestDay }}
                      @if (data.busiestHour !== null) {
                        · {{ data.busiestHour }}:00 UTC
                      }
                    </dd>
                  </div>
                }
                @if (data.lastScanAt) {
                  <div>
                    <dt>Último escaneo</dt>
                    <dd>{{ data.lastScanAt | date: 'dd MMM yyyy, HH:mm' }}</dd>
                  </div>
                }
              </dl>

              @let countries = data.byCountry | keyvalue;
              @if (countries.length) {
                <div class="qr-breakdown">
                  <h3>Por país</h3>
                  <ul>
                    @for (row of countries; track row.key) {
                      <li><span>{{ row.key }}</span><strong>{{ row.value }}</strong></li>
                    }
                  </ul>
                </div>
              }
            } @else {
              <p class="qr-metrics-empty">Sin métricas disponibles para este QR.</p>
            }
          </app-panel>
        </div>
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

  private readonly id = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('id') ?? '')),
    { initialValue: this.route.snapshot.paramMap.get('id') ?? '' },
  );

  protected readonly qr = signal<QrItem | null>(null);
  protected readonly metrics = signal<QrMetricsResponse | null>(null);
  protected readonly campaignName = signal('');
  protected readonly notFound = signal(false);
  protected readonly error = signal('');
  protected readonly copied = signal(false);
  protected readonly confirmDeactivate = signal(false);
  protected readonly confirmDelete = signal(false);

  constructor() {
    this.load();
  }

  private load(): void {
    this.qrApi.get(this.id()).subscribe({
      next: (qr) => {
        this.qr.set(qr);
        this.loadMetrics(qr.id);
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

  private loadMetrics(id: string): void {
    this.qrApi.metrics(id).subscribe({
      next: (metrics) => this.metrics.set(metrics),
      error: () => this.metrics.set(null),
    });
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
