import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiJson, TreeAnalysisPayload } from '../shared/models';

@Injectable({ providedIn: 'root' })
export class TreeAnalysisService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.backendUrl}/api/v1/trees`;

  analyze(payload: TreeAnalysisPayload): Observable<ApiJson> {
    const formData = new FormData();
    formData.append('image', payload.image);
    this.appendOptional(formData, 'farmerId', payload.farmerId);
    this.appendOptional(formData, 'county', payload.county);
    this.appendOptional(formData, 'landAcres', payload.landAcres);
    this.appendOptional(formData, 'location', payload.location);
    this.appendOptional(formData, 'notes', payload.notes);

    return this.http.post<ApiJson>(`${this.baseUrl}/analyze`, formData);
  }

  getHistory(limit = 20, cursor?: string): Observable<ApiJson> {
    let params = new HttpParams().set('limit', limit);

    if (cursor) {
      params = params.set('cursor', cursor);
    }

    return this.http.get<ApiJson>(`${this.baseUrl}/history`, { params });
  }

  getQuota(): Observable<ApiJson> {
    return this.http.get<ApiJson>(`${this.baseUrl}/quota`);
  }

  private appendOptional(formData: FormData, key: string, value: string | number | undefined): void {
    if (value !== undefined && value !== null && `${value}`.trim().length > 0) {
      formData.append(key, `${value}`);
    }
  }
}
