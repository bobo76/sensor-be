# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.5.6 application that polls Arduino devices on a cron schedule (every 15 minutes), stores temperature/humidity data in PostgreSQL, and exposes REST APIs for retrieval.

**Note:** Personal home project for a trusted local network — prioritize functionality and maintainability over enterprise-grade security.

**Stack:** Java 17, Spring Boot (Web, WebFlux, Data JPA), PostgreSQL, Lombok, Maven

## Build & Run Commands

```bash
./mvnw clean install          # Build + run tests
./mvnw spring-boot:run        # Run application
./mvnw test                   # Run all tests
./mvnw test -Dtest=Class#method  # Run single test
./mvnw package -DskipTests    # Package JAR without tests
```

**Docker:**
```bash
./mvnw package && docker build -t sensor-be .
```

**Note:** Tests require PostgreSQL running on localhost:5432 with database `sensorsdb`.

## Architecture

### Data Flow

1. `SensorScheduledServices` runs via cron (`0 0,15,30,45 * * * *`) — at minutes 00, 15, 30, 45 of every hour
2. Queries all active Arduinos from DB, polls each in parallel via `parallelStream()`
3. `ArduinoClient` uses WebFlux `WebClient` to call `http://{hostname}:80/data` (10s timeout)
4. Response JSON is sanitized (unquoted `nan` → `"nan"` via regex), mapped to entity, saved to PostgreSQL
5. Polling summary logged every `sensor.polling.log-interval-hours` hours (default: 6, configurable in `application.properties`)

### REST API

| Endpoint | Description |
|---|---|
| `GET /data/current?machineName={name}` | Live data from Arduino |
| `GET /data/historicalData?machineName={name}&startDate={iso}&endDate={iso}&limit={n}` | Historical range (limit default 1000, max 10000) |
| `GET /arduino/` | List all registered devices |
| `POST /arduino/` | Register new device (rejects duplicate hostnames) |

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

### Key Implementation Details

- **Scheduling:** Cron-based (`@Scheduled(cron = ...)`), NOT fixedDelay — runs at wall-clock times regardless of previous task duration
- **NaN handling:** `ArduinoClient` uses `Pattern` regex to convert unquoted `nan` to `"nan"` before JSON parsing
- **Hostname validation:** `HostnameValidator` utility validates hostnames before HTTP requests
- **Error categorization:** ArduinoClient categorizes failures as network (timeout, DNS), HTTP (4xx/5xx), parsing, or unexpected
- **Log throttling:** `SensorScheduledServices` only logs polling summaries every N hours (configurable via `sensor.polling.log-interval-hours`)
- **DB credentials:** Environment variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` with localhost fallback defaults

### Package Structure

- `controllers/` — REST endpoints (`DataController`, `ArduinoController`)
- `services/` — Scheduling (`SensorScheduledServices`), persistence (`SensorDataService`)
- `restClients/` — `ArduinoClient` (WebClient-based HTTP to Arduinos)
- `repositories/` — JPA repositories
- `entities/` — JPA entities (`Arduino`, `SensorData`)
- `models/` — DTOs (`SensorData`, `SensorDataDto`)
- `mappers/` — `SensorDataMapper` (entity ↔ DTO)
- `config/` — `WebClientConfig`, `CorsConfig`
- `util/` — `HostnameValidator`
- `exception/` — `GlobalExceptionHandler`
