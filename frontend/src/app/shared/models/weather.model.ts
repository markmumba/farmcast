import { ApiJson } from './api-json.model';

export interface WeatherParams {
  lat: number;
  lon: number;
  days: number;
  ai: boolean;
  units: 'metric' | 'imperial';
  lang: string;
}

export interface ForecastRow {
  label: string;
  temperature: string;
  rain: string;
  wind: string;
}

export interface WeatherMetric {
  label: string;
  value: string;
  icon: string;
  tone: 'rain' | 'wind' | 'temp' | 'humidity';
}

export interface WeatherApiState {
  forecast: ApiJson | null;
  daily: ApiJson | null;
  hourly: ApiJson | null;
}
