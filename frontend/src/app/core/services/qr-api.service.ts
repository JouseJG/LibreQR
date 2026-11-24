import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { QrStats } from '../models/api.models';
import {
  QrItem,
  QrMetricsResponse,
  QrPage,
  QrStatus,
  QrUpsert,
} from '../../features/qr/models/qr.model';

export type QrSort = 'createdAt,desc' | 'createdAt,asc' | 'name,asc' | 'name,desc';

/** Cliente de `/api/qr` (REQ-QR-01..07, REQ-MET-03). */
@Injectable({ providedIn: 'root' })
export class QrApiService {
  private readonly http = inject(HttpClient);

  list(
    page = 0,
    size = 5,
    sort: QrSort = 'createdAt,desc',
    search?: string,
    status?: QrStatus,
  ): Observable<QrPage> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    if (search) {
      params = params.set('search', search);
    }
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<QrPage>(`${API_BASE_URL}/qr`, { params });
  }

  /** Escaneos humanos acumulados por lotes (una peticion por pagina de la tabla). */
  scanCounts(ids: string[]): Observable<Record<string, number>> {
    if (ids.length === 0) {
      return of({});
    }
    return this.http.post<Record<string, number>>(`${API_BASE_URL}/qr/scan-counts`, { ids });
  }

  get(id: string): Observable<QrItem> {
    return this.http.get<QrItem>(`${API_BASE_URL}/qr/${id}`);
  }

  create(input: QrUpsert): Observable<QrItem> {
    return this.http.post<QrItem>(`${API_BASE_URL}/qr`, input);
  }

  /** Edicion completa (REQ-QR-03/07): reenvia todos los campos + designConfig. */
  update(id: string, input: QrUpsert): Observable<QrItem> {
    return this.http.put<QrItem>(`${API_BASE_URL}/qr/${id}`, input);
  }

  /** REQ-QR-06: desactivar. */
  deactivate(id: string): Observable<QrItem> {
    return this.http.patch<QrItem>(`${API_BASE_URL}/qr/${id}/deactivate`, {});
  }

  /** Reactivar: solo si la campana asociada esta activa (409 en caso contrario). */
  reactivate(id: string): Observable<QrItem> {
    return this.http.patch<QrItem>(`${API_BASE_URL}/qr/${id}/reactivate`, {});
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/qr/${id}`);
  }

  metrics(id: string, from?: string, to?: string): Observable<QrMetricsResponse> {
    let params = new HttpParams();
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<QrMetricsResponse>(`${API_BASE_URL}/qr/${id}/metrics`, { params });
  }

  /** Estadisticas del perfil de QR (resumen + serie + dispositivos + paises) por rango. */
  stats(id: string, from: string, to: string): Observable<QrStats> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<QrStats>(`${API_BASE_URL}/qr/${id}/stats`, { params });
  }

  /** URL de descarga CSV (serie diaria del rango); la sesion viaja en la cookie. */
  statsExportUrl(id: string, from: string, to: string): string {
    return `${API_BASE_URL}/qr/${id}/stats/export?from=${from}&to=${to}`;
  }
}
