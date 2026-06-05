import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { DividerModule } from 'primeng/divider';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MeterGroupModule } from 'primeng/metergroup';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { AccountService } from '../../services/account.service';
import { HealthService } from '../../services/health.service';
import { TreeAnalysisService } from '../../services/tree-analysis.service';
import { WeatherService } from '../../services/weather.service';
import {
  ApiJson,
  ApiJsonObject,
  CanopyMetric,
  ForecastRow,
  HistoryRow,
  TreeAnalysisPayload,
  WeatherApiState,
  WeatherMetric,
  WeatherParams,
} from '../../shared/models';

interface SelectOption<T extends string = string> {
  label: string;
  value: T;
}

@Component({
  selector: 'app-dashboard',
  imports: [
    ButtonModule,
    CardModule,
    CheckboxModule,
    DividerModule,
    InputNumberModule,
    InputTextModule,
    MessageModule,
    MeterGroupModule,
    ProgressSpinnerModule,
    ReactiveFormsModule,
    SelectModule,
    TableModule,
    TagModule,
    TextareaModule,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly healthService = inject(HealthService);
  private readonly treeAnalysisService = inject(TreeAnalysisService);
  private readonly weatherService = inject(WeatherService);

  readonly heroVideoUrl = 'https://assets.mixkit.co/videos/4075/4075-720.mp4';
  readonly heroPosterUrl = 'https://assets.mixkit.co/videos/4075/4075-thumb-720-0.jpg';

  readonly languageOptions: SelectOption[] = [
    { label: 'English', value: 'en' },
    { label: 'Swahili', value: 'sw' },
  ];
  readonly unitsOptions: SelectOption<'metric' | 'imperial'>[] = [
    { label: 'Metric', value: 'metric' },
    { label: 'Imperial', value: 'imperial' },
  ];

  readonly backendStatus = signal<'checking' | 'online' | 'offline'>('checking');
  readonly backendCheckedAt = signal<string | null>(null);
  readonly weatherState = signal<WeatherApiState>({ forecast: null, daily: null, hourly: null });
  readonly analysis = signal<ApiJson | null>(null);
  readonly quota = signal<ApiJson | null>(null);
  readonly history = signal<ApiJson | null>(null);
  readonly usage = signal<ApiJson | null>(null);
  readonly selectedImage = signal<File | null>(null);
  readonly selectedImagePreview = signal<string | null>(null);

  readonly weatherLoading = signal(false);
  readonly analysisLoading = signal(false);
  readonly quotaLoading = signal(false);
  readonly historyLoading = signal(false);
  readonly usageLoading = signal(false);

  readonly weatherError = signal('');
  readonly analysisError = signal('');
  readonly resourceError = signal('');

  readonly farmForm = this.fb.group({
    farmName: ['Kisumu Demo Farm', [Validators.required]],
    lat: [-0.1022, [Validators.required, Validators.min(-90), Validators.max(90)]],
    lon: [34.7617, [Validators.required, Validators.min(-180), Validators.max(180)]],
    days: [7, [Validators.required, Validators.min(1), Validators.max(16)]],
    units: ['metric' as 'metric' | 'imperial', [Validators.required]],
    lang: ['en', [Validators.required]],
    ai: [true],
  });

  readonly treeForm = this.fb.group({
    farmerId: ['farmcast-demo'],
    county: ['Kisumu'],
    landAcres: [2.5, [Validators.min(0)]],
    location: ['Kisumu Demo Farm'],
    notes: ['Mixed tree line and crop canopy around the farm boundary.'],
  });

  readonly backendTagLabel = computed(() => {
    const status = this.backendStatus();

    if (status === 'checking') {
      return 'Checking API';
    }

    return status === 'online' ? 'API online' : 'API offline';
  });
  readonly backendTagSeverity = computed<'success' | 'warn' | 'danger'>(() => {
    const status = this.backendStatus();

    if (status === 'checking') {
      return 'warn';
    }

    return status === 'online' ? 'success' : 'danger';
  });

  readonly weatherSummary = computed(() => {
    const forecast = this.weatherState().forecast;
    return this.findString(forecast, [
      'summary',
      'aiSummary',
      'ai_summary',
      'data.summary',
      'data.aiSummary',
      'data.ai_summary',
      'ai.summary',
      'ai.text',
      'weather.summary',
    ]) ?? 'Ready for a WeatherAI forecast.';
  });

  readonly weatherMetrics = computed<WeatherMetric[]>(() => {
    const forecast = this.weatherState().forecast;
    const currentHour = this.currentHourlyEntry();

    return [
      {
        label: 'Temperature',
        value: this.formatMetric(this.findNumber(forecast, [
          'temperature',
          'temp',
          'current.temperature',
          'current.temp',
          'data.temperature',
          'data.current.temperature',
          'weather.temperature',
        ]) ?? this.findNumber(currentHour, this.temperaturePaths()), '°C'),
        icon: 'pi pi-sun',
        tone: 'temp',
      },
      {
        label: 'Rain chance',
        value: this.formatRain(forecast, currentHour),
        icon: 'pi pi-cloud',
        tone: 'rain',
      },
      {
        label: 'Wind',
        value: this.formatMetric(this.findNumber(forecast, [
          'wind',
          'windSpeed',
          'wind_speed',
          'current.wind',
          'current.windSpeed',
          'current.wind_speed',
          'data.wind',
          'data.current.windSpeed',
          'data.current.wind_speed',
        ]) ?? this.findNumber(currentHour, this.windPaths()), ' km/h'),
        icon: 'pi pi-compass',
        tone: 'wind',
      },
      {
        label: 'Humidity',
        value: this.formatMetric(this.findNumber(forecast, [
          'humidity',
          'current.humidity',
          'data.humidity',
          'data.current.humidity',
        ]) ?? this.findNumber(currentHour, ['humidity', 'relative_humidity', 'relativeHumidity']), '%'),
        icon: 'pi pi-percentage',
        tone: 'humidity',
      },
    ];
  });

  readonly forecastRows = computed<ForecastRow[]>(() => {
    const rows = this.findArray(this.weatherState().daily, [
      'daily',
      'days',
      'forecast',
      'forecasts',
      'data.daily',
      'data.days',
      'data.forecast',
      'data.forecasts',
      'weather.daily',
    ]);
    const mappedRows = rows.slice(0, 7).map((row, index) => this.toForecastRow(row, index));

    if (mappedRows.some((row) => this.hasForecastValues(row))) {
      return mappedRows;
    }

    const hourlyRows = this.findArray(this.weatherState().hourly ?? this.weatherState().forecast, [
      'hourly',
      'hours',
      'data.hourly',
      'data.hours',
      'weather.hourly',
    ]);
    const aggregatedRows = this.toDailyRowsFromHourly(hourlyRows);

    return aggregatedRows.length > 0 ? aggregatedRows : mappedRows;
  });

  readonly hourlyRows = computed<ForecastRow[]>(() => {
    const rows = this.findArray(this.weatherState().hourly ?? this.weatherState().forecast, [
      'hourly',
      'hours',
      'data.hourly',
      'data.hours',
      'weather.hourly',
    ]);

    return rows.slice(0, 8).map((row, index) => this.toHourlyRow(row, index));
  });

  readonly canopyMetrics = computed<CanopyMetric[]>(() => {
    const analysis = this.analysis();

    return [
      {
        label: 'Tree count',
        value: this.formatPlain(this.findNumber(analysis, [
          'treeCount',
          'tree_count',
          'data.treeCount',
          'data.tree_count',
          'result.treeCount',
          'analysis.treeCount',
        ])),
        detail: 'Detected trees',
      },
      {
        label: 'Canopy',
        value: this.formatMetric(this.findNumber(analysis, [
          'canopyCoverage',
          'canopy_coverage',
          'data.canopyCoverage',
          'data.canopy_coverage',
          'analysis.canopyCoverage',
        ]), '%'),
        detail: 'Coverage',
      },
      {
        label: 'Health',
        value: this.findString(analysis, [
          'health',
          'overallHealth',
          'overall_health',
          'data.health',
          'data.overallHealth',
          'analysis.health',
        ]) ?? '--',
        detail: 'Overall status',
      },
    ];
  });

  readonly overlayImageUrl = computed(() => this.findString(this.analysis(), [
    'overlayImage',
    'overlayUrl',
    'overlay_url',
    'annotatedImage',
    'data.overlayImage',
    'data.overlayUrl',
    'data.overlay_url',
    'result.overlayImage',
  ]));

  readonly recommendations = computed(() => this.findStringArray(this.analysis(), [
    'recommendations',
    'data.recommendations',
    'analysis.recommendations',
    'observations',
    'data.observations',
  ]));

  readonly quotaMeter = computed(() => {
    const quota = this.quota();
    const used = this.findNumber(quota, ['used', 'data.used', 'monthly.used', 'data.monthly.used']) ?? 0;
    const limit = this.findNumber(quota, ['limit', 'data.limit', 'monthly.limit', 'data.monthly.limit']) ?? 0;
    const percentage = limit > 0 ? Math.min(100, Math.round((used / limit) * 100)) : 0;

    return [
      {
        label: 'Used',
        value: percentage,
        color: '#2f7d32',
      },
    ];
  });

  readonly quotaLabel = computed(() => {
    const quota = this.quota();
    const used = this.findNumber(quota, ['used', 'data.used', 'monthly.used', 'data.monthly.used']);
    const limit = this.findNumber(quota, ['limit', 'data.limit', 'monthly.limit', 'data.monthly.limit']);

    if (used === null || limit === null) {
      return '--';
    }

    return `${used} / ${limit}`;
  });

  readonly usageLabel = computed(() => {
    const usage = this.usage();
    const used = this.findNumber(usage, ['requests', 'used', 'data.requests', 'data.used', 'usage.requests']);
    const limit = this.findNumber(usage, ['limit', 'data.limit', 'usage.limit']);

    if (used === null && limit === null) {
      return '--';
    }

    return limit === null ? `${used}` : `${used ?? 0} / ${limit}`;
  });

  readonly historyRows = computed<HistoryRow[]>(() => {
    const rows = this.findArray(this.history(), [
      'items',
      'history',
      'analyses',
      'data.items',
      'data.history',
      'data.analyses',
    ]);

    return rows.slice(0, 6).map((row, index) => ({
      id: this.findString(row, ['id', '_id', 'analysisId', 'analysis_id']) ?? `analysis-${index + 1}`,
      date: this.findString(row, ['createdAt', 'created_at', 'date', 'timestamp']) ?? '--',
      county: this.findString(row, ['county', 'location.county', 'data.county']) ?? '--',
      treeCount: this.formatPlain(this.findNumber(row, ['treeCount', 'tree_count', 'data.treeCount'])),
      health: this.findString(row, ['health', 'overallHealth', 'overall_health']) ?? '--',
    }));
  });

  ngOnInit(): void {
    this.checkBackend();
    this.loadAccountUsage();
    this.loadTreeQuota();
    this.loadTreeHistory();
  }

  fetchWeather(): void {
    if (this.farmForm.invalid) {
      this.farmForm.markAllAsTouched();
      this.weatherError.set('Enter a valid farm location before fetching the forecast.');
      return;
    }

    const params = this.buildWeatherParams();
    this.weatherLoading.set(true);
    this.weatherError.set('');

    forkJoin({
      forecast: this.weatherService.getForecast(params),
      daily: this.weatherService.getDaily(params),
      hourly: this.weatherService.getHourly(params),
    }).subscribe({
      next: (state) => {
        this.weatherState.set(state);
        this.weatherLoading.set(false);
      },
      error: (error: unknown) => {
        this.weatherError.set(this.toErrorMessage(error));
        this.weatherLoading.set(false);
      },
    });
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    this.selectedImage.set(file);
    this.analysisError.set('');

    if (!file) {
      this.selectedImagePreview.set(null);
      return;
    }

    this.selectedImagePreview.set(URL.createObjectURL(file));
  }

  analyzeTrees(): void {
    const image = this.selectedImage();

    if (!image) {
      this.analysisError.set('Select a farm image before running tree analysis.');
      return;
    }

    const treeForm = this.treeForm.getRawValue();
    const payload: TreeAnalysisPayload = {
      image,
      farmerId: treeForm.farmerId,
      county: treeForm.county,
      landAcres: treeForm.landAcres,
      location: treeForm.location,
      notes: treeForm.notes,
    };

    this.analysisLoading.set(true);
    this.analysisError.set('');

    this.treeAnalysisService.analyze(payload).subscribe({
      next: (analysis) => {
        this.analysis.set(analysis);
        this.analysisLoading.set(false);
        this.loadTreeQuota();
        this.loadTreeHistory();
      },
      error: (error: unknown) => {
        this.analysisError.set(this.toErrorMessage(error));
        this.analysisLoading.set(false);
      },
    });
  }

  private checkBackend(): void {
    this.healthService.check().subscribe({
      next: (health) => {
        this.backendStatus.set(health.status === 'UP' ? 'online' : 'offline');
        this.backendCheckedAt.set(health.checkedAt);
      },
      error: () => {
        this.backendStatus.set('offline');
        this.backendCheckedAt.set(null);
      },
    });
  }

  private loadAccountUsage(): void {
    this.usageLoading.set(true);
    this.accountService.getUsage().subscribe({
      next: (usage) => {
        this.usage.set(usage);
        this.usageLoading.set(false);
      },
      error: (error: unknown) => {
        this.resourceError.set(this.toErrorMessage(error));
        this.usageLoading.set(false);
      },
    });
  }

  private loadTreeQuota(): void {
    this.quotaLoading.set(true);
    this.treeAnalysisService.getQuota().subscribe({
      next: (quota) => {
        this.quota.set(quota);
        this.quotaLoading.set(false);
      },
      error: (error: unknown) => {
        this.resourceError.set(this.toErrorMessage(error));
        this.quotaLoading.set(false);
      },
    });
  }

  private loadTreeHistory(): void {
    this.historyLoading.set(true);
    this.treeAnalysisService.getHistory(20).subscribe({
      next: (history) => {
        this.history.set(history);
        this.historyLoading.set(false);
      },
      error: (error: unknown) => {
        this.resourceError.set(this.toErrorMessage(error));
        this.historyLoading.set(false);
      },
    });
  }

  private buildWeatherParams(): WeatherParams {
    const form = this.farmForm.getRawValue();

    return {
      lat: form.lat,
      lon: form.lon,
      days: form.days,
      ai: form.ai,
      units: form.units,
      lang: form.lang,
    };
  }

  private toForecastRow(row: ApiJson, index: number): ForecastRow {
    return {
      label: this.findString(row, ['date', 'day', 'time', 'datetime']) ?? `Day ${index + 1}`,
      temperature: this.formatDailyTemperature(row),
      rain: this.formatRain(row),
      wind: this.formatMetric(this.findNumber(row, this.windPaths()), ' km/h'),
    };
  }

  private toHourlyRow(row: ApiJson, index: number): ForecastRow {
    const time = this.findString(row, ['time', 'datetime', 'date']);

    return {
      label: time ? this.formatTimeLabel(time) : `Hour ${index + 1}`,
      temperature: this.formatTemperature(row),
      rain: this.formatRain(row),
      wind: this.formatMetric(this.findNumber(row, this.windPaths()), ' km/h'),
    };
  }

  private toDailyRowsFromHourly(rows: ApiJson[]): ForecastRow[] {
    const groupedRows = rows.reduce<Record<string, ApiJson[]>>((groups, row) => {
      const time = this.findString(row, ['time', 'datetime', 'date']);
      const date = time?.slice(0, 10);

      if (!date) {
        return groups;
      }

      return {
        ...groups,
        [date]: [...(groups[date] ?? []), row],
      };
    }, {});

    return Object.entries(groupedRows)
      .slice(0, 7)
      .map(([date, dayRows]) => ({
        label: date,
        temperature: this.formatMetric(this.averageValues(dayRows, this.temperaturePaths()), '°C'),
        rain: this.formatMetric(this.maxValues(dayRows, this.rainProbabilityPaths()), '%'),
        wind: this.formatMetric(this.maxValues(dayRows, this.windPaths()), ' km/h'),
      }));
  }

  private hasForecastValues(row: ForecastRow): boolean {
    return row.temperature !== '--' || row.rain !== '--' || row.wind !== '--';
  }

  private currentHourlyEntry(): ApiJson | null {
    const forecast = this.weatherState().forecast;
    const rows = this.findArray(this.weatherState().hourly ?? forecast, [
      'hourly',
      'hours',
      'data.hourly',
      'data.hours',
      'weather.hourly',
    ]);
    const currentTime = this.findString(forecast, ['current.time', 'data.current.time', 'weather.current.time']);
    const currentHour = currentTime?.slice(0, 13);

    if (!currentHour) {
      return rows[0] ?? null;
    }

    return rows.find((row) => this.findString(row, ['time', 'datetime', 'date'])?.slice(0, 13) === currentHour)
      ?? rows[0]
      ?? null;
  }

  private formatTemperature(source: ApiJson | null): string {
    return this.formatMetric(this.findNumber(source, this.temperaturePaths()), '°C');
  }

  private formatDailyTemperature(source: ApiJson | null): string {
    const min = this.findNumber(source, ['temp_min', 'temperature_min', 'minTemp', 'min_temp', 'temperature_2m_min']);
    const max = this.findNumber(source, ['temp_max', 'temperature_max', 'maxTemp', 'max_temp', 'temperature_2m_max']);

    if (min !== null && max !== null) {
      return `${this.formatPlain(min)}-${this.formatPlain(max)}°C`;
    }

    return this.formatTemperature(source);
  }

  private formatRain(source: ApiJson | null, fallbackSource: ApiJson | null = null): string {
    const probability = this.findNumber(source, this.rainProbabilityPaths())
      ?? this.findNumber(fallbackSource, this.rainProbabilityPaths());

    if (probability !== null) {
      return this.formatMetric(probability, '%');
    }

    const amount = this.findNumber(source, this.rainAmountPaths())
      ?? this.findNumber(fallbackSource, this.rainAmountPaths());

    return this.formatMetric(amount, ' mm');
  }

  private temperaturePaths(): string[] {
    return [
      'temperature',
      'temp',
      'avgTemp',
      'avg_temp',
      'average_temperature',
      'temperature_avg',
      'temperatureAvg',
      'maxTemp',
      'max_temp',
      'temp_max',
      'temperature_max',
      'temperatureMax',
      'max_temperature',
      'temp_min',
      'temperature_min',
      'minTemp',
      'min_temp',
      'temperatureMin',
      'min_temperature',
      'temperature_2m_max',
      'temperature_2m_min',
      'apparent_temperature_max',
      'data.temperature',
      'data.temperature_max',
      'data.temp_max',
      'data.temperature_2m_max',
    ];
  }

  private rainProbabilityPaths(): string[] {
    return [
      'precipitation_probability',
      'precipitationProbability',
      'precip_probability',
      'precipProbability',
      'precipitation_probability_max',
      'precipitationProbabilityMax',
      'rain_probability',
      'rainProbability',
      'data.precipitation_probability',
      'data.precipitation_probability_max',
    ];
  }

  private rainAmountPaths(): string[] {
    return [
      'rain',
      'rainfall',
      'rain_sum',
      'rainSum',
      'precipitation',
      'precip',
      'precipitation_sum',
      'precipitationSum',
      'showers_sum',
      'showersSum',
      'current.rain',
      'current.precipitation',
      'data.rain',
      'data.precipitation',
      'data.precipitation_sum',
      'data.current.precipitation',
    ];
  }

  private windPaths(): string[] {
    return [
      'wind',
      'windSpeed',
      'wind_speed',
      'wind_max',
      'windMax',
      'wind_speed_max',
      'windSpeedMax',
      'wind_speed_10m_max',
      'wind_gust',
      'wind_gusts_10m_max',
      'current.wind',
      'current.windSpeed',
      'current.wind_speed',
      'data.wind',
      'data.windSpeed',
      'data.wind_speed',
      'data.current.windSpeed',
      'data.current.wind_speed',
    ];
  }

  private averageValues(rows: ApiJson[], paths: string[]): number | null {
    const values = rows
      .map((row) => this.findNumber(row, paths))
      .filter((value): value is number => value !== null);

    if (values.length === 0) {
      return null;
    }

    return values.reduce((total, value) => total + value, 0) / values.length;
  }

  private maxValues(rows: ApiJson[], paths: string[]): number | null {
    const values = rows
      .map((row) => this.findNumber(row, paths))
      .filter((value): value is number => value !== null);

    return values.length > 0 ? Math.max(...values) : null;
  }

  private formatTimeLabel(value: string): string {
    if (!value.includes('T')) {
      return value;
    }

    return value.slice(11, 16);
  }

  private findValue(source: ApiJson | null, paths: string[]): ApiJson | undefined {
    for (const path of paths) {
      const value = this.readPath(source, path);

      if (value !== undefined && value !== null) {
        return value;
      }
    }

    return undefined;
  }

  private findString(source: ApiJson | null, paths: string[]): string | null {
    const value = this.findValue(source, paths);

    if (typeof value === 'string' && value.trim().length > 0) {
      return value;
    }

    if (typeof value === 'number' || typeof value === 'boolean') {
      return `${value}`;
    }

    return null;
  }

  private findNumber(source: ApiJson | null, paths: string[]): number | null {
    const value = this.findValue(source, paths);

    if (typeof value === 'number') {
      return value;
    }

    if (typeof value === 'string') {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : null;
    }

    return null;
  }

  private findStringArray(source: ApiJson | null, paths: string[]): string[] {
    const value = this.findValue(source, paths);

    if (Array.isArray(value)) {
      return value
        .map((item) => (typeof item === 'string' ? item : this.findString(item, ['text', 'message', 'label', 'description'])))
        .filter((item): item is string => Boolean(item));
    }

    const singleValue = this.findString(source, paths);
    return singleValue ? [singleValue] : [];
  }

  private findArray(source: ApiJson | null, paths: string[]): ApiJson[] {
    const value = this.findValue(source, paths);

    if (Array.isArray(value)) {
      return value;
    }

    return this.findFirstArray(source);
  }

  private findFirstArray(source: ApiJson | null): ApiJson[] {
    if (Array.isArray(source)) {
      return source;
    }

    if (!this.isObject(source)) {
      return [];
    }

    for (const value of Object.values(source)) {
      const nested = this.findFirstArray(value);
      if (nested.length > 0) {
        return nested;
      }
    }

    return [];
  }

  private readPath(source: ApiJson | null, path: string): ApiJson | undefined {
    if (source === null || source === undefined) {
      return undefined;
    }

    return path.split('.').reduce<ApiJson | undefined>((current, key) => {
      if (!this.isObject(current)) {
        return undefined;
      }

      return current[key];
    }, source);
  }

  private isObject(value: ApiJson | undefined): value is ApiJsonObject {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
  }

  private formatMetric(value: number | null, suffix: string): string {
    if (value === null) {
      return '--';
    }

    return `${Math.round(value * 10) / 10}${suffix}`;
  }

  private formatPlain(value: number | null): string {
    if (value === null) {
      return '--';
    }

    return `${Math.round(value * 10) / 10}`;
  }

  private toErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const errorBody = error.error as unknown;

      if (typeof errorBody === 'string' && errorBody.trim().length > 0) {
        return errorBody;
      }

      if (this.isObject(errorBody as ApiJson)) {
        const message = this.findString(errorBody as ApiJson, ['message', 'error', 'detail']);
        if (message) {
          return message;
        }
      }

      return `Request failed with status ${error.status}`;
    }

    return 'Request failed. Check the backend and API key configuration.';
  }
}
