package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.repositories.ArduinoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ArduinoService {

    private final ArduinoRepository arduinoRepository;

    public List<Arduino> findAll() {
        return arduinoRepository.findAll();
    }

    public List<Arduino> findActiveArduinos() {
        return arduinoRepository.findByIsActiveTrue();
    }

    public Optional<Arduino> addArduino(Arduino arduino) {
        try {
            return Optional.of(arduinoRepository.save(arduino));
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }
}
