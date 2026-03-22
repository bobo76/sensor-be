package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.mappers.SensorDataMapper;
import com.house.sensors.sensors.restClients.ArduinoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorScheduledServicesTest {

    @Mock
    private ArduinoClient arduinoClient;

    @Mock
    private ArduinoService arduinoService;

    @Mock
    private SensorDataService sensorDataService;

    @Mock
    private SensorDataMapper sensorDataMapper;

    @InjectMocks
    private SensorScheduledServices scheduledServices;

    private Arduino arduino1;
    private Arduino arduino2;
    private com.house.sensors.sensors.models.SensorData modelData;
    private SensorData entityData;

    @BeforeEach
    void setUp() {
        arduino1 = new Arduino();
        arduino1.setId(1L);
        arduino1.setHostName("192.168.1.100");
        arduino1.setIsActive(true);

        arduino2 = new Arduino();
        arduino2.setId(2L);
        arduino2.setHostName("192.168.1.101");
        arduino2.setIsActive(true);

        modelData =
            com.house.sensors.sensors.models.SensorData.builder()
                .machineName("192.168.1.100")
                .temperature("22.5")
                .humidity("45.0")
                .creationDate(Instant.now())
                .build();

        entityData = SensorData.builder()
                .machineName("192.168.1.100")
                .temperature("22.5")
                .humidity("45.0")
                .creationDate(Instant.now())
                .build();
    }

    @Test
    void runTask_shouldPollAllActiveArduinos() {
        // Arrange
        List<Arduino> activeArduinos =
            Arrays.asList(arduino1, arduino2);
        when(arduinoService.findActiveArduinos())
            .thenReturn(activeArduinos);
        when(arduinoClient.getSensorData(anyString()))
            .thenReturn(Optional.of(modelData));
        when(sensorDataMapper.toSensorDataEntity(any()))
            .thenReturn(entityData);
        when(sensorDataService.saveSensorData(any()))
            .thenReturn(entityData);

        // Act
        scheduledServices.runTask();

        // Assert
        verify(arduinoService).findActiveArduinos();
        verify(arduinoClient, times(2))
            .getSensorData(anyString());
        verify(sensorDataMapper, times(2))
            .toSensorDataEntity(any());
        verify(sensorDataService, times(2))
            .saveSensorData(any());
    }

    @Test
    void runTask_shouldHandleEmptyArduinoList() {
        // Arrange
        when(arduinoService.findActiveArduinos())
            .thenReturn(List.of());

        // Act
        scheduledServices.runTask();

        // Assert
        verify(arduinoService).findActiveArduinos();
        verify(arduinoClient, never())
            .getSensorData(anyString());
        verify(sensorDataService, never())
            .saveSensorData(any());
    }

    @Test
    void runTask_shouldContinueOnClientError() {
        // Arrange
        List<Arduino> activeArduinos =
            Arrays.asList(arduino1, arduino2);
        when(arduinoService.findActiveArduinos())
            .thenReturn(activeArduinos);
        when(arduinoClient.getSensorData("192.168.1.100"))
            .thenReturn(Optional.empty());
        when(arduinoClient.getSensorData("192.168.1.101"))
            .thenReturn(Optional.of(modelData));
        when(sensorDataMapper.toSensorDataEntity(any()))
            .thenReturn(entityData);
        when(sensorDataService.saveSensorData(any()))
            .thenReturn(entityData);

        // Act
        scheduledServices.runTask();

        // Assert
        verify(arduinoClient).getSensorData("192.168.1.100");
        verify(arduinoClient).getSensorData("192.168.1.101");
        verify(sensorDataService, times(1))
            .saveSensorData(any());
    }

    @Test
    void runTask_shouldHandleExceptionDuringPolling() {
        // Arrange
        List<Arduino> activeArduinos =
            Arrays.asList(arduino1, arduino2);
        when(arduinoService.findActiveArduinos())
            .thenReturn(activeArduinos);
        when(arduinoClient.getSensorData("192.168.1.100"))
            .thenThrow(new RuntimeException("Network error"));
        when(arduinoClient.getSensorData("192.168.1.101"))
            .thenReturn(Optional.of(modelData));
        when(sensorDataMapper.toSensorDataEntity(any()))
            .thenReturn(entityData);
        when(sensorDataService.saveSensorData(any()))
            .thenReturn(entityData);

        // Act
        scheduledServices.runTask();

        // Assert
        verify(arduinoClient).getSensorData("192.168.1.100");
        verify(arduinoClient).getSensorData("192.168.1.101");
        verify(sensorDataService, times(1))
            .saveSensorData(any());
    }

    @Test
    void onApplicationReady_shouldLogStartup() {
        // Act - verifies method executes without errors
        scheduledServices.onApplicationReady();
    }
}
