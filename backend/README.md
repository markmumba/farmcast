# FarmCast AI Backend

Spring Boot API adapter for the WeatherAI developer platform.

## Run

```bash
cp .env.example .env
# Fill WEATHER_AI_API_KEY in .env
mvn spring-boot:run
```

The API starts on `http://localhost:8080`.

## Environment

`application.yml` imports `backend/.env` when it exists. Supported values:

- `SERVER_PORT`
- `FRONTEND_URL`
- `WEATHER_AI_BASE_URL`
- `WEATHER_AI_API_KEY`

## Endpoints

- `GET /api/health`
- `GET /api/v1/weather`
- `GET /api/v1/weather/current`
- `GET /api/v1/weather/daily`
- `GET /api/v1/weather/hourly`
- `POST /api/v1/trees/analyze`
- `GET /api/v1/trees/history`
- `GET /api/v1/trees/quota`
- `GET /api/v1/account/usage`

All WeatherAI endpoints are proxied by the backend so the frontend never receives the WeatherAI API key.

### Weather Query Parameters

- `lat` required, `-90` to `90`
- `lon` required, `-180` to `180`
- `days` optional, default `7`, max `16`
- `ai` optional, default `true`
- `units` optional, `metric` or `imperial`
- `lang` optional, default `en`

### Tree Analysis Form Fields

Send `multipart/form-data` to `POST /api/v1/trees/analyze`.

- `image` required, JPEG/PNG/WEBP, max 20 MB
- `farmerId` optional
- `county` optional
- `landAcres` optional
- `location` optional
- `notes` optional

## Structure

The backend follows the same module-oriented layout as the CloudIt backend:

- `common/response` shared API response helpers
- `config` application configuration
- `config/properties` typed configuration properties
- `modules/<feature>/controller` REST controllers
- `modules/<feature>/dto` request/response DTOs
- `modules/<feature>/service` business logic
