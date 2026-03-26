package com.house.sensors.sensors.controllers;

import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.mappers.SensorDataMapper;
import com.house.sensors.sensors.models.AggregatedDataResponse;
import com.house.sensors.sensors.models.AggregatedSensorDataDto;
import com.house.sensors.sensors.models.AggregationTier;
import com.house.sensors.sensors.models.HistoricalAggregatedDataRequest;
import com.house.sensors.sensors.models.HistoricalDataRequest;
import com.house.sensors.sensors.models.SensorDataDto;
import com.house.sensors.sensors.restClients.ArduinoClient;
import com.house.sensors.sensors.services.SensorDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataControllerTest {

    @Mock
    private ArduinoClient arduinoClient;

    @Mock
    private SensorDataService sensorDataService;

    @Mock
    private SensorDataMapper sensorDataMapper;

    @InjectMocks
    private DataController dataController;

    private com.house.sensors.sensors.models.SensorData modelSensorData;
    private SensorData entitySensorData;
    private SensorDataDto sensorDataDto;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();

        modelSensorData =
            com.house.sensors.sensors.models.SensorData.builder()
                .machineName("arduino1")
                .temperature("22.5")
                .humidity("45.0")
                .creationDate(now)
                .build();

        entitySensorData = SensorData.builder()
            .id(1L)
            .machineName("arduino1")
            .temperature("22.5")
            .humidity("45.0")
            .creationDate(now)
            .build();

        sensorDataDto = SensorDataDto.builder()
            .machineName("arduino1")
            .temperature("22.5")
            .humidity("45.0")
            .creationDate(now)
            .hasError(false)
            .build();
    }

    @Test
    void currentData_shouldReturnSensorData_whenArduinoResponds() {
        // Arrange
        when(arduinoClient.getSensorData("arduino1"))
            .thenReturn(Optional.of(modelSensorData));

        // Act
        ResponseEntity<com.house.sensors.sensors.models.SensorData>
            response = dataController.currentData("arduino1");

        // Assert
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMachineName())
            .isEqualTo("arduino1");
        assertThat(response.getBody().getTemperature())
            .isEqualTo("22.5");
        assertThat(response.getBody().getHumidity())
            .isEqualTo("45.0");
        verify(arduinoClient).getSensorData("arduino1");
    }

    @Test
    void currentData_shouldReturnNotFound_whenArduinoDoesNotRespond() {
        // Arrange
        when(arduinoClient.getSensorData("arduino1"))
            .thenReturn(Optional.empty());

        // Act
        ResponseEntity<com.house.sensors.sensors.models.SensorData>
            response = dataController.currentData("arduino1");

        // Assert
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(arduinoClient).getSensorData("arduino1");
    }

    @Test
    void historicalData_shouldReturnData_whenValidDateRange() {
        // Arrange
        Instant startDate = now.minusSeconds(3600);
        Instant endDate = now;
        List<SensorData> entities =
            Collections.singletonList(entitySensorData);

        when(sensorDataService.findHistoricalData(
                eq("arduino1"), eq(startDate),
                eq(endDate), eq(1000)))
            .thenReturn(entities);
        when(sensorDataMapper.toSensorDataDto(
                any(SensorData.class)))
            .thenReturn(sensorDataDto);

        // Act
        HistoricalDataRequest request = HistoricalDataRequest.builder()
            .machineName("arduino1")
            .startDate(startDate)
            .endDate(endDate)
            .limit(1000)
            .build();
        ResponseEntity<List<SensorDataDto>> response =
            dataController.historicalData(request);

        // Assert
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().getMachineName())
            .isEqualTo("arduino1");
        verify(sensorDataService).findHistoricalData(
            eq("arduino1"), eq(startDate),
            eq(endDate), eq(1000));
        verify(sensorDataMapper).toSensorDataDto(entitySensorData);
    }

    @Test
    void historicalData_shouldReturnBadRequest_whenStartDateAfterEndDate() {
        // Arrange
        Instant startDate = now;
        Instant endDate = now.minusSeconds(3600);

        // Act
        HistoricalDataRequest request = HistoricalDataRequest.builder()
            .machineName("arduino1")
            .startDate(startDate)
            .endDate(endDate)
            .limit(1000)
            .build();
        ResponseEntity<List<SensorDataDto>> response =
            dataController.historicalData(request);

        // Assert
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
        verify(sensorDataService, never())
            .findHistoricalData(any(), any(), any(), anyInt());
    }

    @Test
    void historicalData_shouldReturnEmptyList_whenNoDataFound() {
        // Arrange
        Instant startDate = now.minusSeconds(3600);
        Instant endDate = now;

        when(sensorDataService.findHistoricalData(
                eq("arduino1"), eq(startDate),
                eq(endDate), eq(1000)))
            .thenReturn(List.of());

        // Act
        HistoricalDataRequest request = HistoricalDataRequest.builder()
            .machineName("arduino1")
            .startDate(startDate)
            .endDate(endDate)
            .limit(1000)
            .build();
        ResponseEntity<List<SensorDataDto>> response =
            dataController.historicalData(request);

        // Assert
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void historicalAggregated_shouldReturnData_whenValidDateRange() {
        // Arrange
        Instant startDate = now.minusSeconds(3600);
        Instant endDate = now;
        AggregatedDataResponse aggregatedResponse =
            AggregatedDataResponse.builder()
                .aggregationTier(AggregationTier.RAW)
                .data(List.of(AggregatedSensorDataDto.builder()
                    .bucketTimestamp(now)
                    .machineName("arduino1")
                    .avgTemperature(22.5)
                    .avgHumidity(45.0)
                    .sampleCount(1L)
                    .build()))
                .build();

        when(sensorDataService.findAggregatedHistoricalData(
                eq("arduino1"), eq(startDate), eq(endDate)))
            .thenReturn(aggregatedResponse);

        // Act
        HistoricalAggregatedDataRequest request =
            HistoricalAggregatedDataRequest.builder()
                .machineName("arduino1")
                .startDate(startDate)
                .endDate(endDate)
                .build();
        ResponseEntity<AggregatedDataResponse> response =
            dataController.historicalAggregated(request);

        // Assert
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAggregationTier())
            .isEqualTo(AggregationTier.RAW);
        assertThat(response.getBody().getData()).hasSize(1);
        verify(sensorDataService)
            .findAggregatedHistoricalData(
                eq("arduino1"), eq(startDate), eq(endDate));
    }

    @Test
    void historicalAggregated_shouldReturnBadRequest_whenStartDateAfterEndDate() {
        // Arrange
        Instant startDate = now;
        Instant endDate = now.minusSeconds(3600);

        // Act
        HistoricalAggregatedDataRequest request =
            HistoricalAggregatedDataRequest.builder()
                .machineName("arduino1")
                .startDate(startDate)
                .endDate(endDate)
                .build();
        ResponseEntity<AggregatedDataResponse> response =
            dataController.historicalAggregated(request);

        // Assert
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
        verify(sensorDataService, never())
            .findAggregatedHistoricalData(
                any(), any(), any());
    }
}
