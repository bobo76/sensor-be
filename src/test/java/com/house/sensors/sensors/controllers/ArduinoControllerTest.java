package com.house.sensors.sensors.controllers;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.mappers.ArduinoMapper;
import com.house.sensors.sensors.models.ArduinoDto;
import com.house.sensors.sensors.services.ArduinoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArduinoControllerTest {

    @Mock
    private ArduinoService arduinoService;

    @Mock
    private ArduinoMapper arduinoMapper;

    @InjectMocks
    private ArduinoController arduinoController;

    private Arduino arduino1;
    private Arduino arduino2;
    private ArduinoDto dto1;
    private ArduinoDto dto2;

    @BeforeEach
    void setUp() {
        arduino1 = new Arduino();
        arduino1.setId(1L);
        arduino1.setHostName("192.168.1.100");
        arduino1.setIsActive(true);

        arduino2 = new Arduino();
        arduino2.setId(2L);
        arduino2.setHostName("arduino.local");
        arduino2.setIsActive(false);

        dto1 = ArduinoDto.builder()
            .id(1L)
            .hostName("192.168.1.100")
            .isActive(true)
            .build();

        dto2 = ArduinoDto.builder()
            .id(2L)
            .hostName("arduino.local")
            .isActive(false)
            .build();
    }

    @Test
    void getArduinos_shouldReturnAllArduinos() {
        // Arrange
        when(arduinoService.findAll())
            .thenReturn(List.of(arduino1, arduino2));
        when(arduinoMapper.toDto(arduino1)).thenReturn(dto1);
        when(arduinoMapper.toDto(arduino2)).thenReturn(dto2);

        // Act
        List<ArduinoDto> result =
            arduinoController.getArduinos();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(dto1, dto2);
        verify(arduinoService).findAll();
    }

    @Test
    void getArduinos_shouldReturnEmptyList_whenNoArduinos() {
        // Arrange
        when(arduinoService.findAll()).thenReturn(List.of());

        // Act
        List<ArduinoDto> result =
            arduinoController.getArduinos();

        // Assert
        assertThat(result).isEmpty();
        verify(arduinoService).findAll();
    }

    @Test
    void addArduino_shouldSaveAndReturnCreated() {
        // Arrange
        ArduinoDto request = ArduinoDto.builder()
            .hostName("192.168.1.101")
            .isActive(true)
            .build();

        Arduino entity = new Arduino();
        entity.setHostName("192.168.1.101");
        entity.setIsActive(true);

        Arduino savedEntity = new Arduino();
        savedEntity.setId(3L);
        savedEntity.setHostName("192.168.1.101");
        savedEntity.setIsActive(true);

        ArduinoDto savedDto = ArduinoDto.builder()
            .id(3L)
            .hostName("192.168.1.101")
            .isActive(true)
            .build();

        when(arduinoMapper.toEntity(request)).thenReturn(entity);
        when(arduinoService.addArduino(entity))
            .thenReturn(Optional.of(savedEntity));
        when(arduinoMapper.toDto(savedEntity))
            .thenReturn(savedDto);

        // Act
        ResponseEntity<ArduinoDto> response =
            arduinoController.addArduino(request);

        // Assert
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(3L);
        assertThat(response.getBody().getHostName())
            .isEqualTo("192.168.1.101");
        assertThat(response.getBody().getIsActive()).isTrue();
    }

    @Test
    void addArduino_shouldReturnBadRequest_whenHostNameAlreadyExists() {
        // Arrange
        ArduinoDto request = ArduinoDto.builder()
            .hostName("192.168.1.100")
            .isActive(true)
            .build();

        Arduino entity = new Arduino();
        entity.setHostName("192.168.1.100");
        entity.setIsActive(true);

        when(arduinoMapper.toEntity(request)).thenReturn(entity);
        when(arduinoService.addArduino(entity))
            .thenReturn(Optional.empty());

        // Act
        ResponseEntity<ArduinoDto> response =
            arduinoController.addArduino(request);

        // Assert
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }
}
