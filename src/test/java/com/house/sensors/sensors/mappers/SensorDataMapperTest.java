package com.house.sensors.sensors.mappers;

import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.models.SensorDataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SensorDataMapperTest {

    private SensorDataMapper mapper;
    private Instant now;

    @BeforeEach
    void setUp() {
        mapper = new SensorDataMapper();
        now = Instant.now();
    }

    @Test
    void toSensorDataEntity_shouldMapModelToEntity() {
        // Arrange
        com.house.sensors.sensors.models.SensorData model =
                com.house.sensors.sensors.models.SensorData.builder()
                        .machineName("arduino1")
                        .temperature("22.5")
                        .humidity("45.0")
                        .creationDate(now)
                        .build();

        // Act
        SensorData entity = mapper.toSensorDataEntity(model);

        // Assert
        assertThat(entity).isNotNull();
        assertThat(entity.getMachineName()).isEqualTo("arduino1");
        assertThat(entity.getTemperature()).isEqualTo("22.5");
        assertThat(entity.getHumidity()).isEqualTo("45.0");
        assertThat(entity.getCreationDate()).isEqualTo(now);
    }

    @Test
    void toSensorDataEntity_shouldReturnNull_whenInputIsNull() {
        // Act
        SensorData result = mapper.toSensorDataEntity(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void toSensorDataDto_shouldMapEntityToDto_withoutError() {
        // Arrange
        SensorData entity = SensorData.builder()
                .id(1L)
                .machineName("arduino1")
                .temperature("22.5")
                .humidity("45.0")
                .creationDate(now)
                .build();

        // Act
        SensorDataDto dto = mapper.toSensorDataDto(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.getMachineName()).isEqualTo("arduino1");
        assertThat(dto.getTemperature()).isEqualTo("22.5");
        assertThat(dto.getHumidity()).isEqualTo("45.0");
        assertThat(dto.getCreationDate()).isEqualTo(now);
        assertThat(dto.isHasError()).isFalse();
    }

    @Test
    void toSensorDataDto_shouldDetectError_whenTemperatureIsNan() {
        // Arrange
        SensorData entity = SensorData.builder()
                .id(1L)
                .machineName("arduino1")
                .temperature("nan")
                .humidity("45.0")
                .creationDate(now)
                .build();

        // Act
        SensorDataDto dto = mapper.toSensorDataDto(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.isHasError()).isTrue();
    }

    @Test
    void toSensorDataDto_shouldDetectError_whenHumidityIsNan() {
        // Arrange
        SensorData entity = SensorData.builder()
                .id(1L)
                .machineName("arduino1")
                .temperature("22.5")
                .humidity("nan")
                .creationDate(now)
                .build();

        // Act
        SensorDataDto dto = mapper.toSensorDataDto(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.isHasError()).isTrue();
    }

    @Test
    void toSensorDataDto_shouldDetectError_whenBothAreNan() {
        // Arrange
        SensorData entity = SensorData.builder()
                .id(1L)
                .machineName("arduino1")
                .temperature("nan")
                .humidity("nan")
                .creationDate(now)
                .build();

        // Act
        SensorDataDto dto = mapper.toSensorDataDto(entity);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.isHasError()).isTrue();
    }

    @Test
    void toSensorDataDto_shouldDetectError_caseInsensitive() {
        // Arrange - Test with NaN, NAN, Nan
        SensorData entity1 = SensorData.builder()
                .machineName("arduino1")
                .temperature("NaN")
                .humidity("45.0")
                .creationDate(now)
                .build();

        SensorData entity2 = SensorData.builder()
                .machineName("arduino2")
                .temperature("22.5")
                .humidity("NAN")
                .creationDate(now)
                .build();

        // Act
        SensorDataDto dto1 = mapper.toSensorDataDto(entity1);
        SensorDataDto dto2 = mapper.toSensorDataDto(entity2);

        // Assert
        assertThat(dto1.isHasError()).isTrue();
        assertThat(dto2.isHasError()).isTrue();
    }

    @Test
    void toSensorDataDto_shouldReturnNull_whenInputIsNull() {
        // Act
        SensorDataDto result = mapper.toSensorDataDto(null);

        // Assert
        assertThat(result).isNull();
    }
}
