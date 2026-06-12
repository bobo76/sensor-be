package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.exception.DuplicateResourceException;
import com.house.sensors.sensors.repositories.ArduinoRepository;
import com.house.sensors.sensors.util.HostnameValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArduinoServiceTest {

    @Mock
    private ArduinoRepository arduinoRepository;

    private ArduinoService arduinoService;

    private Arduino arduino1;
    private Arduino arduino2;

    @BeforeEach
    void setUp() {
        arduinoService = new ArduinoService(
                arduinoRepository, new HostnameValidator());

        arduino1 = new Arduino();
        arduino1.setId(1L);
        arduino1.setHostName("192.168.1.100");
        arduino1.setIsActive(true);

        arduino2 = new Arduino();
        arduino2.setId(2L);
        arduino2.setHostName("192.168.1.101");
        arduino2.setIsActive(false);
    }

    @Test
    void findAll_shouldReturnAllArduinos() {
        // Arrange
        when(arduinoRepository.findAll())
            .thenReturn(List.of(arduino1, arduino2));

        // Act
        List<Arduino> result = arduinoService.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result)
            .containsExactly(arduino1, arduino2);
        verify(arduinoRepository).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoArduinos() {
        // Arrange
        when(arduinoRepository.findAll())
            .thenReturn(List.of());

        // Act
        List<Arduino> result = arduinoService.findAll();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveArduinos_shouldReturnOnlyActive() {
        // Arrange
        when(arduinoRepository.findByIsActiveTrue())
            .thenReturn(List.of(arduino1));

        // Act
        List<Arduino> result =
            arduinoService.findActiveArduinos();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getHostName())
            .isEqualTo("192.168.1.100");
        verify(arduinoRepository).findByIsActiveTrue();
    }

    @Test
    void addArduino_shouldSaveAndReturn() {
        // Arrange
        Arduino newArduino = new Arduino();
        newArduino.setHostName("192.168.1.102");
        newArduino.setIsActive(true);

        Arduino saved = new Arduino();
        saved.setId(3L);
        saved.setHostName("192.168.1.102");
        saved.setIsActive(true);

        when(arduinoRepository.save(any(Arduino.class)))
            .thenReturn(saved);

        // Act
        Arduino result = arduinoService.addArduino(newArduino);

        // Assert
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getHostName())
            .isEqualTo("192.168.1.102");
        verify(arduinoRepository).save(newArduino);
    }

    @Test
    void addArduino_shouldThrowDuplicate_whenHostnameExists() {
        // Arrange
        Arduino duplicate = new Arduino();
        duplicate.setHostName("192.168.1.100");
        duplicate.setIsActive(true);

        when(arduinoRepository.save(any(Arduino.class)))
            .thenThrow(
                new DataIntegrityViolationException(
                    "Unique constraint violated"));

        // Act + Assert
        assertThatThrownBy(
            () -> arduinoService.addArduino(duplicate))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("192.168.1.100");
        verify(arduinoRepository).save(duplicate);
    }

    @Test
    void addArduino_shouldThrowIllegalArgument_whenHostnameInvalid() {
        // Arrange
        Arduino invalid = new Arduino();
        invalid.setHostName("not a hostname!!");
        invalid.setIsActive(true);

        // Act + Assert
        assertThatThrownBy(
            () -> arduinoService.addArduino(invalid))
            .isInstanceOf(IllegalArgumentException.class);
        verify(arduinoRepository, never()).save(any());
    }
}
