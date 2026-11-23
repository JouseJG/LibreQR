import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { CampaignScan, DailyScanPoint, StatsSummary } from '../models/api.models';

/** Cliente de `/api/stats` (resumen y graficas del dashboard). */
@Injectable({ providedIn: 'root' })
export class StatsApiService {
  private readonly http = inject(HttpClient);

  summary(): Observable<StatsSummary> {
    return this.http.get<StatsSummary>(`${API_BASE_URL}/stats/summary`);
  }

  dailyScans(): Observable<DailyScanPoint[]> {
    return this.http.get<DailyScanPoint[]>(`${API_BASE_URL}/stats/daily-scans`);
  }

  campaignScans(): Observable<CampaignScan[]> {
    return this.http.get<CampaignScan[]>(`${API_BASE_URL}/stats/campaign-scans`);
  }
}
