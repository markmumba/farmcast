import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiJson } from '../shared/models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.backendUrl}/api/v1/account`;

  getUsage(): Observable<ApiJson> {
    return this.http.get<ApiJson>(`${this.baseUrl}/usage`);
  }
}
