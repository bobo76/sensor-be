package com.house.sensors.sensors.mappers;

import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.models.SensorDataDto;
import org.springframework.stereotype.Component;

@Component
public class SensorDataMapper {

    private static final String ERROR_VALUE = "nan";

    public SensorData toSensorDataEntity(com.house.sensors.sensors.models.SensorData value) {
        if (value == null) {
            return null;
        }
        return SensorData.builder()
                .creationDate(value.getCreationDate())
                .humidity(value.getHumidity())
                .machineName(value.getMachineName())
                .temperature(value.getTemperature())
                .build();
    }

    public SensorDataDto toSensorDataDto(SensorData value) {
        if (value == null) {
            return null;
        }
        return SensorDataDto.builder()
                .creationDate(value.getCreationDate())
                .hasError(ERROR_VALUE.equalsIgnoreCase(value.getHumidity()) || ERROR_VALUE.equalsIgnoreCase(value.getTemperature()))
                .humidity(value.getHumidity())
                .machineName(value.getMachineName())
                .temperature(value.getTemperature())
                .build();
    }
}
