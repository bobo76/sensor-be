package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.repositories.SensorDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorDataServiceTest {

    @Mock
    private SensorDataRepository sensorDataRepository;

    @InjectMocks
    private SensorDataService sensorDataService;

    private SensorData sensorData;

    @BeforeEach
    void setUp() {
        sensorData = SensorData.builder()
                .machineName("arduino1")
                .temperature("22.5")
                .humidity("45.0")
                .creationDate(Instant.now())
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
}
