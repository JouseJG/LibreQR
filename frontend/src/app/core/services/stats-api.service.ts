import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { CampaignScan, DailyScanPoint, StatsSummary } from '../models/api.models';

export type StatsRange = { from: string; to: string };

/** Cliente de `/api/stats` (resumen y graficas del dashboard, con rango opcional). */
@Injectable({ providedIn: 'root' })
export class StatsApiService {
  private readonly http = inject(HttpClient);

  summary(range?: StatsRange): Observable<StatsSummary> {
    return this.http.get<StatsSummary>(`${API_BASE_URL}/stats/summary`, { params: toParams(range) });
  }

  dailyScans(range?: StatsRange): Observable<DailyScanPoint[]> {
    return this.http.get<DailyScanPoint[]>(`${API_BASE_URL}/stats/daily-scans`, {
      params: toParams(range),
    });
  }

  campaignScans(range?: StatsRange): Observable<CampaignScan[]> {
    return this.http.get<CampaignScan[]>(`${API_BASE_URL}/stats/campaign-scans`, {
      params: toParams(range),
    });
  }
}

function toParams(range?: StatsRange): HttpParams {
  let params = new HttpParams();
  if (range) {
    params = params.set('from', range.from).set('to', range.to);
  }
  return params;
}
