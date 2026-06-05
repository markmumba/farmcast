import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LoadingService {
  private readonly loading = signal(false);

  readonly isLoading = this.loading.asReadonly();

  setIsLoading(isLoading: boolean): void {
    this.loading.set(isLoading);
  }
}
