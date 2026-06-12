package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.models.AggregatedDataResponse;
import com.house.sensors.sensors.models.AggregatedSensorDataDto;
import com.house.sensors.sensors.models.AggregationTier;
import com.house.sensors.sensors.repositories.AggregatedSensorDataRepository;
import com.house.sensors.sensors.repositories.SensorDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorDataServiceTest {

    private static final int RAW_TIER_MAX_RESULTS = 10_000;

    @Mock
    private SensorDataRepository sensorDataRepository;

    @Mock
    private AggregatedSensorDataRepository
        aggregatedSensorDataRepository;

    @Mock
    private AggregationTierResolver aggregationTierResolver;

    @InjectMocks
    private SensorDataService sensorDataService;

    private SensorData sensorData;
    private Instant start;
    private Instant end;

    @BeforeEach
    void setUp() {
        start = Instant.parse("2026-05-01T00:00:00Z");
        end = Instant.parse("2026-05-02T00:00:00Z");
        sensorData = SensorData.builder()
                .machineName("arduino1")
                .temperature("22.5")
                .humidity("45.0")
                .creationDate(start)
                .build();
    }

    @Test
    void saveSensorData_shouldSaveAndReturnData() {
        // Arrange
        SensorData savedData = SensorData.builder()
                .id(1L)
                .machineName("arduino1")
                .temperature("22.5")
                .humidity("45.0")
                .creationDate(sensorData.getCreationDate())
                .build();

        when(sensorDataRepository.save(any(SensorData.class))).thenReturn(savedData);

        // Act
        SensorData result = sensorDataService.saveSensorData(sensorData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMachineName()).isEqualTo("arduino1");
        assertThat(result.getTemperature()).isEqualTo("22.5");
        assertThat(result.getHumidity()).isEqualTo("45.0");
        verify(sensorDataRepository).save(sensorData);
    }

    @Test
    void saveSensorData_shouldHandleNanValues() {
        // Arrange
        SensorData dataWithNan = SensorData.builder()
                .machineName("arduino2")
                .temperature("nan")
                .humidity("nan")
                .creationDate(Instant.now())
                .build();

        SensorData savedData = SensorData.builder()
                .id(2L)
                .machineName("arduino2")
                .temperature("nan")
                .humidity("nan")
                .creationDate(dataWithNan.getCreationDate())
                .build();

        when(sensorDataRepository.save(any(SensorData.class))).thenReturn(savedData);

        // Act
        SensorData result = sensorDataService.saveSensorData(dataWithNan);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTemperature()).isEqualTo("nan");
        assertThat(result.getHumidity()).isEqualTo("nan");
        verify(sensorDataRepository).save(dataWithNan);
    }

    @Test
    void findAggregated_shouldUseRawRepository_whenRawTier() {
        // Arrange
        when(aggregationTierResolver.resolve(start, end))
            .thenReturn(AggregationTier.RAW);
        when(sensorDataRepository
                .findByMachineNameAndCreationDateBetweenOrderByCreationDateAsc(
                    eq("arduino1"), eq(start), eq(end),
                    any(Pageable.class)))
            .thenReturn(List.of(sensorData));

        // Act
        AggregatedDataResponse response =
            sensorDataService.findAggregatedHistoricalData(
                "arduino1", start, end);

        // Assert
        assertThat(response.getAggregationTier())
            .isEqualTo(AggregationTier.RAW);
        assertThat(response.isTruncated()).isFalse();
        assertThat(response.getData()).hasSize(1);
        AggregatedSensorDataDto dto = response.getData().getFirst();
        assertThat(dto.getAvgTemperature()).isEqualTo(22.5);
        assertThat(dto.getAvgHumidity()).isEqualTo(45.0);
        assertThat(dto.getSampleCount()).isEqualTo(1L);
        verifyNoInteractions(aggregatedSensorDataRepository);
    }

    @Test
    void findAggregated_shouldNullOutNan_inRawTier() {
        // Arrange
        SensorData nanData = SensorData.builder()
            .machineName("arduino1")
            .temperature("nan")
            .humidity("45.0")
            .creationDate(start)
            .build();
        when(aggregationTierResolver.resolve(start, end))
            .thenReturn(AggregationTier.RAW);
        when(sensorDataRepository
                .findByMachineNameAndCreationDateBetweenOrderByCreationDateAsc(
                    eq("arduino1"), eq(start), eq(end),
                    any(Pageable.class)))
            .thenReturn(List.of(nanData));

        // Act
        AggregatedDataResponse response =
            sensorDataService.findAggregatedHistoricalData(
                "arduino1", start, end);

        // Assert
        AggregatedSensorDataDto dto = response.getData().getFirst();
        assertThat(dto.getAvgTemperature()).isNull();
        assertThat(dto.getAvgHumidity()).isEqualTo(45.0);
    }

    @Test
    void findAggregated_shouldMarkTruncated_whenCapReached() {
        // Arrange
        when(aggregationTierResolver.resolve(start, end))
            .thenReturn(AggregationTier.RAW);
        when(sensorDataRepository
                .findByMachineNameAndCreationDateBetweenOrderByCreationDateAsc(
                    eq("arduino1"), eq(start), eq(end),
                    any(Pageable.class)))
            .thenReturn(Collections.nCopies(
                RAW_TIER_MAX_RESULTS, sensorData));

        // Act
        AggregatedDataResponse response =
            sensorDataService.findAggregatedHistoricalData(
                "arduino1", start, end);

        // Assert
        assertThat(response.isTruncated()).isTrue();
    }

    @Test
    void findAggregated_shouldDelegateToAggregatedRepo_whenNonRawTier() {
        // Arrange
        AggregatedSensorDataDto bucket =
            AggregatedSensorDataDto.builder()
                .bucketTimestamp(start)
                .machineName("arduino1")
                .avgTemperature(20.0)
                .sampleCount(4L)
                .build();
        when(aggregationTierResolver.resolve(start, end))
            .thenReturn(AggregationTier.HOURLY);
        when(aggregatedSensorDataRepository.findAggregated(
                "arduino1", start, end, AggregationTier.HOURLY))
            .thenReturn(List.of(bucket));

        // Act
        AggregatedDataResponse response =
            sensorDataService.findAggregatedHistoricalData(
                "arduino1", start, end);

        // Assert
        assertThat(response.getAggregationTier())
            .isEqualTo(AggregationTier.HOURLY);
        assertThat(response.isTruncated()).isFalse();
        assertThat(response.getData()).containsExactly(bucket);
        verify(sensorDataRepository, never())
            .findByMachineNameAndCreationDateBetweenOrderByCreationDateAsc(
                any(), any(), any(), any());
    }
}
