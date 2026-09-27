package com.house.sensors.sensors.mappers;

import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.models.SensorDataDto;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class SensorDataMapper {

    private static final Set<String> ERROR_VALUES =
        Set.of("nan", "inf", "-inf", "ovf");

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
                .hasError(isErrorValue(value.getHumidity())
                    || isErrorValue(value.getTemperature()))
                .humidity(value.getHumidity())
                .machineName(value.getMachineName())
                .temperature(value.getTemperature())
                .build();
    }

    private boolean isErrorValue(String value) {
        return value != null
            && ERROR_VALUES.contains(value.toLowerCase(Locale.ROOT));
    }
}
