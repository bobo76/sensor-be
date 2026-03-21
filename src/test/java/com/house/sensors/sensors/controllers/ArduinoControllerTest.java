package com.house.sensors.sensors.controllers;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.repositories.ArduinoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArduinoControllerTest {

    @Mock
    private ArduinoRepository arduinoRepository;

    @InjectMocks
    private ArduinoController arduinoController;

    private Arduino arduino1;
    private Arduino arduino2;

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
    }

    @Test
    void getArduinos_shouldReturnAllArduinos() {
        // Arrange
        List<Arduino> arduinos = Arrays.asList(arduino1, arduino2);
        when(arduinoRepository.findAll()).thenReturn(arduinos);

        // Act
        List<Arduino> result = arduinoController.getArduinos();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(arduino1, arduino2);
        verify(arduinoRepository).findAll();
    }

    @Test
    void getArduinos_shouldReturnEmptyList_whenNoArduinos() {
        // Arrange
        when(arduinoRepository.findAll()).thenReturn(List.of());

        // Act
        List<Arduino> result = arduinoController.getArduinos();

        // Assert
        assertThat(result).isEmpty();
        verify(arduinoRepository).findAll();
    }

    @Test
    void addArduino_shouldSaveAndReturnArduino() {
        // Arrange
        Arduino newArduino = new Arduino();
        newArduino.setHostName("192.168.1.101");
        newArduino.setIsActive(true);

        Arduino savedArduino = new Arduino();
        savedArduino.setId(3L);
        savedArduino.setHostName("192.168.1.101");
        savedArduino.setIsActive(true);

        when(arduinoRepository.existsByHostName("192.168.1.101")).thenReturn(false);
        when(arduinoRepository.save(any(Arduino.class))).thenReturn(savedArduino);

        // Act
        ResponseEntity<Arduino> response = arduinoController.addArduino(newArduino);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(3L);
        assertThat(response.getBody().getHostName()).isEqualTo("192.168.1.101");
        assertThat(response.getBody().getIsActive()).isTrue();
        verify(arduinoRepository).save(newArduino);
    }

    @Test
    void addArduino_shouldReturnBadRequest_whenHostNameAlreadyExists() {
        // Arrange
        Arduino newArduino = new Arduino();
        newArduino.setHostName("192.168.1.100");
        newArduino.setIsActive(true);

        when(arduinoRepository.existsByHostName("192.168.1.100")).thenReturn(true);

        // Act
        ResponseEntity<Arduino> response = arduinoController.addArduino(newArduino);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
        verify(arduinoRepository, never()).save(any());
    }
}
