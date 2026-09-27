# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.0.8 application that polls Arduino devices on a cron schedule (every 15 minutes), stores temperature/humidity data in PostgreSQL, and exposes REST APIs for retrieval.

**Note:** Personal home project for a trusted local network — prioritize functionality and maintainability over enterprise-grade security.

**Stack:** Java 25, Spring Boot (Web, Data JPA, Actuator), PostgreSQL, Lombok, Maven

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

**Note:** The test suite is pure unit tests (Mockito) and does not require a running PostgreSQL instance.

**Mockito agent:** `maven-dependency-plugin` (`properties` goal) + surefire `argLine` load Mockito as a `-javaagent` at test JVM startup, avoiding the JDK dynamic-agent-loading warnings. If a plugin that sets `argLine` (e.g. JaCoCo) is added, prefix it: `@{argLine} -javaagent:...`.

## Architecture

### Data Flow

1. `SensorScheduledServices` runs via cron (`0 0,15,30,45 * * * *`) — at minutes 00, 15, 30, 45 of every hour
2. Queries all active Arduinos from DB, polls each in parallel via a virtual-thread-per-task executor
3. `ArduinoClient` uses `RestClient` (JDK `HttpClient`) to call `http://{hostname}:80/data` (5s connect / 10s response timeout)
4. Response JSON is sanitized (unquoted `nan`/`inf`/`ovf` → quoted strings via regex), mapped to entity, saved to PostgreSQL
5. Polling summary logged every `sensor.polling.log-interval-hours` hours (default: 6, configurable in `application.properties`)

### REST API

| Endpoint | Description |
|---|---|
| `GET /data/current?machineName={name}` | Live data from Arduino |
| `GET /data/historicalData?machineName={name}&startDate={iso}&endDate={iso}&limit={n}` | Historical range (limit default 1000, max 10000) |
| `GET /arduino/` | List all registered devices |
| `POST /arduino/` | Register new device (rejects duplicate hostnames) |

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

**Actuator:** `/actuator/health` (details always shown, includes DB), `/actuator/info` (build info), `/actuator/metrics` (e.g. `/actuator/metrics/jvm.memory.used?tag=area:heap`). Exposure set via `management.endpoints.web.exposure.include` in `application.properties`.

### Key Implementation Details

- **Scheduling:** Cron-based (`@Scheduled(cron = ...)`), NOT fixedDelay — runs at wall-clock times regardless of previous task duration
- **NaN handling:** `ArduinoClient` uses `Pattern` regex to quote the bare `nan`/`inf`/`ovf` tokens Arduino's `printFloat` emits before JSON parsing; `SensorValueParser.NUMERIC_REGEX` is the single definition of a numeric reading, shared by the RAW tier and the aggregation SQL
- **Aggregation time zone:** bucket boundaries use `sensor.aggregation.time-zone` (env `SENSOR_TIME_ZONE`, default `America/New_York`), independent of JVM/DB zone
- **Hostname validation:** `HostnameValidator` utility validates hostnames before HTTP requests
- **Error categorization:** ArduinoClient categorizes failures as network (timeout, DNS), HTTP (4xx/5xx), parsing, or unexpected
- **Log throttling:** `SensorScheduledServices` only logs polling summaries every N hours (configurable via `sensor.polling.log-interval-hours`)
- **Error responses:** `GlobalExceptionHandler` returns a uniform `ErrorResponse(status, error, message, timestamp)`. Specific handlers map framework exceptions to proper statuses — 400 (validation, missing/mismatched params, malformed JSON body), 404 (`NoResourceFoundException`, logged at DEBUG only, e.g. browser `/favicon.ico`), 405 (with `Allow` header), 409 (duplicate), 415 (with `Accept` header). The `Exception` catch-all returns 500 with a stack trace, so any new framework exception that should not be a 500 needs its own handler.
- **DB credentials:** Environment variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` with localhost fallback defaults

### Package Structure

- `controllers/` — REST endpoints (`DataController`, `ArduinoController`)
- `services/` — Scheduling (`SensorScheduledServices`), persistence (`SensorDataService`)
- `restClients/` — `ArduinoClient` (RestClient-based HTTP to Arduinos)
- `repositories/` — JPA repositories
- `entities/` — JPA entities (`Arduino`, `SensorData`)
- `models/` — DTOs (`SensorData`, `SensorDataDto`)
- `mappers/` — `SensorDataMapper` (entity ↔ DTO)
- `config/` — `RestClientConfig`, `CorsConfig`
- `util/` — `HostnameValidator`
- `exception/` — `GlobalExceptionHandler`
