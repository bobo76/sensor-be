package com.house.sensors.sensors.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedSensorDataDto {
    private Instant bucketTimestamp;
    private String machineName;
    private Double avgTemperature;
    private Double avgHumidity;
    private Double minTemperature;
    private Double maxTemperature;
    private Double minHumidity;
    private Double maxHumidity;
    private Long sampleCount;
}
