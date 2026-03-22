package com.house.sensors.sensors.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SensorData {
    private Instant creationDate;
    private String machineName;
    private String temperature;
    private String humidity;
}
