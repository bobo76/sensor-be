package com.house.sensors.sensors.controllers;

import com.house.sensors.sensors.mappers.SensorDataMapper;
import com.house.sensors.sensors.models.SensorData;
import com.house.sensors.sensors.models.SensorDataDto;
import com.house.sensors.sensors.repositories.SensorDataRepository;
import com.house.sensors.sensors.restClients.ArduinoClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Tag(name = "Sensor Data", description = "APIs for retrieving sensor data from Arduino devices")
@Validated
@RequiredArgsConstructor
@RestController()
@RequestMapping("/data")
public class DataController {
    private final ArduinoClient client;
    private final SensorDataRepository sensorDataRepository;
    private final SensorDataMapper sensorDataMapper;

    @Operation(summary = "Get current sensor data", description = "Fetches live sensor data directly from an Arduino device")
    @GetMapping("/current")
    public ResponseEntity<SensorData> currentData(
            @Parameter(description = "Arduino device hostname or IP address", required = true)
            @RequestParam @NotBlank String machineName) {
        final Optional<SensorData> sensor = client.getSensorData(machineName);
        return sensor.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get historical sensor data", description = "Retrieves historical sensor data from database for a specific time range")
    @GetMapping("/historicalData")
    public ResponseEntity<List<SensorDataDto>> historicalData(
            @Parameter(description = "Arduino device name", required = true)
            @RequestParam @NotBlank String machineName,
            @Parameter(description = "Start date in ISO-8601 format", required = true)
            @RequestParam @NotNull Instant startDate,
            @Parameter(description = "End date in ISO-8601 format", required = true)
            @RequestParam @NotNull Instant endDate,
            @Parameter(description = "Maximum number of results (default 1000, max 10000)")
            @RequestParam(defaultValue = "1000") @Positive @Max(10000) int limit) {
        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(sensorDataRepository.findByMachineNameAndCreationDateBetween(
                        machineName, startDate, endDate, PageRequest.of(0, limit))
                .stream().map(sensorDataMapper::toSensorDataDto).toList());
    }
}