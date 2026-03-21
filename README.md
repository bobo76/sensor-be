# Sensor Data Collection System

A Spring Boot application that collects temperature and humidity data from Arduino devices via HTTP polling and stores the data in PostgreSQL for historical analysis.

## Overview

This system automatically polls registered Arduino devices every 15 minutes, retrieves sensor readings (temperature and humidity), and stores them in a PostgreSQL database. It provides REST APIs for retrieving current sensor data and querying historical data ranges.

## Features

- ✅ Scheduled polling of Arduino devices every 15 minutes
- ✅ Automatic handling of invalid sensor readings (NaN values)
- ✅ REST API for current and historical data retrieval
- ✅ Arduino device registration and management
- ✅ Parallel processing of multiple devices
- ✅ Error handling and detailed logging
- ✅ Database indexing for efficient queries

## Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6+ (or use included Maven wrapper `./mvnw`)

## Database Setup

1. **Install PostgreSQL** (if not already installed)

2. **Create the database:**
   ```sql
   CREATE DATABASE sensorsdb;
   ```

3. **Create a user** (optional but recommended):
   ```sql
   CREATE USER sensor_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE sensorsdb TO sensor_user;
   ```

## Configuration

### Application Properties

Configure the database connection in `src/main/resources/application.properties` or use environment variables:

**Option 1: Environment Variables (Recommended for Production)**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/sensorsdb
export DB_USERNAME=sensor_user
export DB_PASSWORD=your_password
```

**Option 2: Edit application.properties**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sensorsdb
spring.datasource.username=sensor_user
spring.datasource.password=your_password
```

### Server Port

The application runs on port **8081** by default. Change in `application.properties`:
```properties
server.port=8081
```

## Building and Running

### Using Maven Wrapper (Recommended)

**Build:**
```bash
./mvnw clean install
```

**Run:**
```bash
./mvnw spring-boot:run
```

**Package as JAR:**
```bash
./mvnw package
java -jar target/sensors-0.0.1-SNAPSHOT.jar
```

### Using Docker

**Build:**
```bash
./mvnw package
docker build -t sensor-be .
```

**Run:**
```bash
docker run -p 8081:8081 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/sensorsdb \
  -e DB_USERNAME=sensor_user \
  -e DB_PASSWORD=your_password \
  sensor-be
```

## API Endpoints

### Arduino Management

#### Get All Arduinos
```http
GET /arduino/
```

**Response:**
```json
[
  {
    "id": 1,
    "hostName": "bedroom.local",
    "isActive": true,
    "creationDate": "2025-12-01T10:00:00Z"
  }
]
```

#### Register New Arduino
```http
POST /arduino/
Content-Type: application/json

{
  "hostName": "living-room.local",
  "isActive": true
}
```

### Sensor Data

#### Get Current Data
```http
GET /data/current?machineName=bedroom.local
```

**Response:**
```json
{
  "machineName": "bedroom.local",
  "temperature": "22.5",
  "humidity": "45.2",
  "creationDate": "2025-12-01T15:30:00Z"
}
```

#### Get Historical Data
```http
GET /data/historicalData?machineName=bedroom.local&startDate=2025-12-01T00:00:00Z&endDate=2025-12-01T23:59:59Z
```

**Response:**
```json
[
  {
    "machineName": "bedroom.local",
    "temperature": "22.5",
    "humidity": "45.2",
    "creationDate": "2025-12-01T15:30:00Z",
    "hasError": false
  },
  {
    "machineName": "bedroom.local",
    "temperature": "nan",
    "humidity": "nan",
    "creationDate": "2025-12-01T15:45:00Z",
    "hasError": true
  }
]
```

**Query Parameters:**
- `machineName` (required): Arduino hostname
- `startDate` (required): ISO 8601 timestamp
- `endDate` (required): ISO 8601 timestamp (must be after startDate)

## Arduino Device Setup

Your Arduino devices must expose an HTTP endpoint that returns sensor data in JSON format:

**Expected Arduino Endpoint:**
```
http://{hostname}:80/data
```

**Expected Response Format:**
```json
{
  "temperature": "22.5",
  "humidity": "45.2"
}
```

**Note:** The application handles invalid readings like `nan` (unquoted) automatically.

## Architecture

### Components

- **Controllers** (`controllers/`) - REST API endpoints
- **Services** (`services/`) - Business logic and scheduled tasks
- **Repositories** (`repositories/`) - Database access layer
- **Entities** (`entities/`) - JPA database entities
- **Models** (`models/`) - DTOs for API responses
- **Mappers** (`mappers/`) - Entity ↔ DTO conversion
- **REST Clients** (`restClients/`) - Arduino HTTP communication

### Scheduled Polling

The `SensorScheduledServices` component automatically:
1. Queries all active Arduino devices from the database
2. Polls each device in parallel using HTTP GET
3. Sanitizes and validates the response data
4. Stores successful readings in the database
5. Logs success/failure summary

**Schedule:** Every 15 minutes after the previous task completes (`fixedDelay`)

### Database Schema

**Arduino Table:**
- `id` - Primary key
- `hostName` - DNS name or IP address
- `isActive` - Whether to poll this device
- `creationDate` - Auto-generated timestamp

**SensorData Table:**
- `id` - Primary key
- `machineName` - Arduino hostname
- `temperature` - Temperature reading (string)
- `humidity` - Humidity reading (string)
- `creationDate` - When reading was taken

**Indexes:**
- `idx_hostname` on Arduino(hostName)
- `idx_machine_date` on SensorData(machineName, creationDate)

## Development

### Running Tests
```bash
./mvnw test
```

**Note:** Tests require PostgreSQL to be running and accessible.

### Code Structure
See [.claude/CLAUDE.md](CLAUDE.md) for detailed development guidelines and architecture documentation.

### Common Development Commands

**Clean build:**
```bash
./mvnw clean install
```

**Run single test:**
```bash
./mvnw test -Dtest=ClassName#methodName
```

**Skip tests:**
```bash
./mvnw package -DskipTests
```

## Logging

Application logs show polling activity:

```
INFO  SensorScheduledServices - Starting sensor polling for 3 active Arduino(s)
ERROR SensorScheduledServices - Failed to poll Arduino 'bedroom.local': Connection timed out
INFO  SensorScheduledServices - Sensor polling complete: 2/3 successful
```

## Troubleshooting

### Common Issues

**1. Database Connection Failed**
- Verify PostgreSQL is running: `sudo systemctl status postgresql`
- Check credentials in environment variables or `application.properties`
- Ensure database exists: `psql -l`

**2. Arduino Polling Fails**
- Verify Arduino is reachable: `curl http://arduino-hostname:80/data`
- Check Arduino is registered and `isActive = true`
- Review logs for specific error messages

**3. Port Already in Use**
- Change port in `application.properties`: `server.port=8082`
- Or kill process using port 8081: `lsof -ti:8081 | xargs kill`

**4. JSON Parsing Errors**
- Application handles unquoted `nan` values automatically
- Ensure Arduino returns valid JSON structure

## Production Considerations

- [ ] Configure CORS for specific frontend origins
- [ ] Use connection pooling settings appropriate for your load
- [ ] Set up external configuration (Spring Cloud Config, etc.)
- [ ] Implement proper logging (e.g., Logback with file rotation)
- [ ] Add health check endpoint for monitoring
- [ ] Consider pagination for historical data queries with large datasets
- [ ] Set up database backups
- [ ] Use HTTPS for Arduino communication if possible
- [ ] Monitor database connection pool usage

## License

This project is private/proprietary.

## Support

For issues or questions, please contact the development team.
