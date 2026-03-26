package com.house.sensors.sensors.controllers;

import com.house.sensors.sensors.mappers.SensorDataMapper;
import com.house.sensors.sensors.models.AggregatedDataResponse;
import com.house.sensors.sensors.models.HistoricalAggregatedDataRequest;
import com.house.sensors.sensors.models.HistoricalDataRequest;
import com.house.sensors.sensors.models.SensorData;
import com.house.sensors.sensors.models.SensorDataDto;
import com.house.sensors.sensors.restClients.ArduinoClient;
import com.house.sensors.sensors.services.SensorDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Tag(name = "Sensor Data",
    description = "APIs for retrieving sensor data from Arduino devices")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/data")
public class DataController {
    private final ArduinoClient client;
    private final SensorDataService sensorDataService;
    private final SensorDataMapper sensorDataMapper;

    @Operation(summary = "Get current sensor data",
        description = "Fetches live sensor data directly from "
            + "an Arduino device")
    @GetMapping("/current")
    public ResponseEntity<SensorData> currentData(
            @Parameter(
                description = "Arduino device hostname or IP",
                required = true)
            @RequestParam @NotBlank String machineName) {
        Optional<SensorData> sensor =
            client.getSensorData(machineName);
        return sensor.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get historical sensor data",
        description = "Retrieves historical sensor data from "
            + "database for a specific time range")
    @GetMapping("/historicalData")
    public ResponseEntity<List<SensorDataDto>> historicalData(
            @Valid HistoricalDataRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
            sensorDataService.findHistoricalData(
                    request.getMachineName(),
                    request.getStartDate(),
                    request.getEndDate(),
                    request.getLimit())
                .stream()
                .map(sensorDataMapper::toSensorDataDto)
                .toList());
    }

    @Operation(summary = "Get aggregated historical sensor data",
        description = "Retrieves historical sensor data aggregated "
            + "by time buckets. Aggregation tier is auto-detected "
            + "from the requested time range.")
    @GetMapping("/historicalAggregated")
    public ResponseEntity<AggregatedDataResponse>
            historicalAggregated(
            @Valid HistoricalAggregatedDataRequest request) {
        if (request.getStartDate()
                .isAfter(request.getEndDate())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
            sensorDataService.findAggregatedHistoricalData(
                request.getMachineName(),
                request.getStartDate(),
                request.getEndDate()));
    }
}