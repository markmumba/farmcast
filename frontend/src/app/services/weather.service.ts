import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiJson, WeatherParams } from '../shared/models';

@Injectable({ providedIn: 'root' })
export class WeatherService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.backendUrl}/api/v1/weather`;

  getForecast(params: WeatherParams): Observable<ApiJson> {
    return this.http.get<ApiJson>(this.baseUrl, { params: this.toHttpParams(params) });
  }

  getDaily(params: WeatherParams): Observable<ApiJson> {
    return this.http.get<ApiJson>(`${this.baseUrl}/daily`, { params: this.toHttpParams(params) });
  }

  getHourly(params: WeatherParams): Observable<ApiJson> {
    return this.http.get<ApiJson>(`${this.baseUrl}/hourly`, { params: this.toHttpParams(params) });
  }

  private toHttpParams(params: WeatherParams): HttpParams {
    return new HttpParams()
      .set('lat', params.lat)
      .set('lon', params.lon)
      .set('days', params.days)
      .set('ai', params.ai)
      .set('units', params.units)
      .set('lang', params.lang);
  }
}
