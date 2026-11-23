import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, of, switchMap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { CampaignMetrics } from '../models/api.models';
import { QrApiService } from './qr-api.service';
import {
  Campaign,
  CampaignPage,
  CampaignQrLink,
  CampaignStatus,
  CampaignUpsert,
} from '../../features/campaign/models/campaign.model';
import { QrItem } from '../../features/qr/models/qr.model';

export type CampaignSort = 'createdAt,desc' | 'createdAt,asc' | 'name,asc' | 'name,desc';

/** Cliente de `/api/campaigns` (REQ-CAM-01..05, REQ-MET-04). */
@Injectable({ providedIn: 'root' })
export class CampaignApiService {
  private readonly http = inject(HttpClient);
  private readonly qrApi = inject(QrApiService);

  list(
    page = 0,
    size = 5,
    sort: CampaignSort = 'createdAt,desc',
    search?: string,
    status?: CampaignStatus,
  ): Observable<CampaignPage> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    if (search) {
      params = params.set('search', search);
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<CampaignPage>(`${API_BASE_URL}/campaigns`, { params });
  }

  /** Pausar/reactivar/finalizar (PATCH /{id}/status). */
  changeStatus(id: string, status: CampaignStatus): Observable<Campaign> {
    return this.http.patch<Campaign>(`${API_BASE_URL}/campaigns/${id}/status`, { status });
  }

  get(id: string): Observable<Campaign> {
    return this.http.get<Campaign>(`${API_BASE_URL}/campaigns/${id}`);
  }

  create(input: CampaignUpsert): Observable<Campaign> {
    return this.http.post<Campaign>(`${API_BASE_URL}/campaigns`, input);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/campaigns/${id}`);
  }

  // --- Asociacion campana-QR (REQ-CAM-02..05) ---

  addQr(campaignId: string, qrId: string): Observable<CampaignQrLink> {
    return this.http.post<CampaignQrLink>(`${API_BASE_URL}/campaigns/${campaignId}/qrs/${qrId}`, {});
  }

  /** Quitar el QR de la campana sin borrarlo (REQ-CAM-05). */
  removeQr(campaignId: string, qrId: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/campaigns/${campaignId}/qrs/${qrId}`);
  }

  links(campaignId: string): Observable<CampaignQrLink[]> {
    return this.http.get<CampaignQrLink[]>(`${API_BASE_URL}/campaigns/${campaignId}/qrs`);
  }

  /** Links + detalle de cada QR (N+1 aceptable con campanas pequenas). */
  linkedQrs(campaignId: string): Observable<QrItem[]> {
    return this.links(campaignId).pipe(
      switchMap((links) =>
        links.length === 0
          ? of([] as QrItem[])
          : forkJoin(links.map((link) => this.qrApi.get(link.qrCodeId))),
      ),
    );
  }

  metrics(campaignId: string): Observable<CampaignMetrics> {
    return this.http.get<CampaignMetrics>(`${API_BASE_URL}/campaigns/${campaignId}/metrics`);
  }
}
