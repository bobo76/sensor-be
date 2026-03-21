package com.house.sensors.sensors.models;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Builder
@Data
public class SensorDataDto {
    private Instant creationDate;
    private String machineName;
    private String temperature;
    private String humidity;
    private boolean hasError;
}
