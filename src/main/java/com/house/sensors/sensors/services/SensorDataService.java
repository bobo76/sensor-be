package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.models.AggregatedDataResponse;
import com.house.sensors.sensors.models.AggregatedSensorDataDto;
import com.house.sensors.sensors.models.AggregationTier;
import com.house.sensors.sensors.repositories.AggregatedSensorDataRepository;
import com.house.sensors.sensors.repositories.SensorDataRepository;
import com.house.sensors.sensors.util.SensorValueParser;
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
        boolean truncated = false;
        if (tier == AggregationTier.RAW) {
            // Fetch newest-first with one extra row so we keep the most
            // recent readings (not the oldest) when the cap is hit, and
            // can distinguish "exactly at cap" from "over cap".
            List<SensorData> rawData = sensorDataRepository
                .findByMachineNameAndCreationDateBetweenOrderByCreationDateDesc(
                    machineName, startDate, endDate,
                    PageRequest.of(0, RAW_TIER_MAX_RESULTS + 1));
            truncated = rawData.size() > RAW_TIER_MAX_RESULTS;
            // Drop fully unparseable rows to match the aggregated SQL,
            // then reverse to ascending so charts render chronologically.
            data = rawData.stream()
                .limit(RAW_TIER_MAX_RESULTS)
                .map(entity -> mapToAggregated(entity, machineName))
                .filter(this::hasAnyReading)
                .toList()
                .reversed();
        } else {
            data = aggregatedSensorDataRepository.findAggregated(
                machineName, startDate, endDate, tier);
        }

        return AggregatedDataResponse.builder()
            .aggregationTier(tier)
            .data(data)
            .truncated(truncated)
            .build();
    }

    private AggregatedSensorDataDto mapToAggregated(
            SensorData entity, String machineName) {
        Double temp = SensorValueParser.parse(entity.getTemperature());
        Double hum = SensorValueParser.parse(entity.getHumidity());
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
            .temperatureSampleCount(temp != null ? 1L : 0L)
            .humiditySampleCount(hum != null ? 1L : 0L)
            .build();
    }

    private boolean hasAnyReading(AggregatedSensorDataDto dto) {
        return dto.getAvgTemperature() != null
            || dto.getAvgHumidity() != null;
    }
}
