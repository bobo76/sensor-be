package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.models.AggregatedDataResponse;
import com.house.sensors.sensors.models.AggregatedSensorDataDto;
import com.house.sensors.sensors.models.AggregationTier;
import com.house.sensors.sensors.repositories.AggregatedSensorDataRepository;
import com.house.sensors.sensors.repositories.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SensorDataService {

    private static final int RAW_TIER_MAX_RESULTS = 10_000;

    private final SensorDataRepository sensorDataRepository;
    private final AggregatedSensorDataRepository
        aggregatedSensorDataRepository;
    private final AggregationTierResolver aggregationTierResolver;

    public SensorData saveSensorData(SensorData sensorData) {
        return sensorDataRepository.save(sensorData);
    }

    @Transactional(readOnly = true)
    public List<SensorData> findHistoricalData(
            String machineName,
            Instant startDate,
            Instant endDate,
            int limit) {
        return sensorDataRepository
            .findByMachineNameAndCreationDateBetweenOrderByCreationDateDesc(
                machineName, startDate, endDate,
                PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public AggregatedDataResponse findAggregatedHistoricalData(
            String machineName,
            Instant startDate,
            Instant endDate) {
        AggregationTier tier =
            aggregationTierResolver.resolve(startDate, endDate);

        List<AggregatedSensorDataDto> data;
        if (tier == AggregationTier.RAW) {
            data = findHistoricalData(
                    machineName, startDate, endDate,
                    RAW_TIER_MAX_RESULTS)
                .stream()
                .map(entity -> mapToAggregated(entity, machineName))
                .toList();
        } else {
            data = aggregatedSensorDataRepository.findAggregated(
                machineName, startDate, endDate, tier);
        }

        return AggregatedDataResponse.builder()
            .aggregationTier(tier)
            .data(data)
            .build();
    }

    private AggregatedSensorDataDto mapToAggregated(
            SensorData entity, String machineName) {
        Double temp = parseDouble(entity.getTemperature());
        Double hum = parseDouble(entity.getHumidity());
        return AggregatedSensorDataDto.builder()
            .bucketTimestamp(entity.getCreationDate())
            .machineName(machineName)
            .avgTemperature(temp)
            .minTemperature(temp)
            .maxTemperature(temp)
            .avgHumidity(hum)
            .minHumidity(hum)
            .maxHumidity(hum)
            .sampleCount(1L)
            .build();
    }

    private Double parseDouble(String value) {
        if (value == null
                || value.equalsIgnoreCase("nan")) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}