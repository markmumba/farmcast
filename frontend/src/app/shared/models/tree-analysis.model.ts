import { ApiJson } from './api-json.model';

export interface TreeAnalysisPayload {
  image: File;
  farmerId?: string;
  county?: string;
  landAcres?: number;
  location?: string;
  notes?: string;
}

export interface CanopyMetric {
  label: string;
  value: string;
  detail: string;
}

export interface HistoryRow {
  id: string;
  date: string;
  county: string;
  treeCount: string;
  health: string;
}

export interface TreeApiState {
  analysis: ApiJson | null;
  history: ApiJson | null;
  quota: ApiJson | null;
}
