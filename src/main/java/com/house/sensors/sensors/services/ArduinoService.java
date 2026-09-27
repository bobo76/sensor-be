package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.exception.DuplicateResourceException;
import com.house.sensors.sensors.exception.InvalidRequestException;
import com.house.sensors.sensors.repositories.ArduinoRepository;
import com.house.sensors.sensors.util.HostnameValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ArduinoService {

    private final ArduinoRepository arduinoRepository;
    private final HostnameValidator hostnameValidator;

    public List<Arduino> findAll() {
        return arduinoRepository.findAll();
    }

    public List<Arduino> findActiveArduinos() {
        return arduinoRepository.findByIsActiveTrue();
    }

    public Arduino addArduino(Arduino arduino) {
        HostnameValidator.ValidationResult result =
            hostnameValidator.validateFormat(arduino.getHostName());
        if (!result.isValid()) {
            throw new InvalidRequestException(result.errorMessage());
        }
        arduino.setHostName(
            HostnameValidator.normalize(arduino.getHostName()));
        if (arduinoRepository.existsByHostNameIgnoreCase(
                arduino.getHostName())) {
            throw duplicateHostname(arduino.getHostName());
        }
        try {
            return arduinoRepository.save(arduino);
        } catch (DataIntegrityViolationException e) {
            // Safety net for a concurrent insert that slips past the
            // existsByHostNameIgnoreCase check; hostname is the only unique
            // constraint, so this is always a duplicate.
            throw duplicateHostname(arduino.getHostName());
        }
    }

    private DuplicateResourceException duplicateHostname(String hostName) {
        return new DuplicateResourceException(
            "Arduino with hostname '" + hostName + "' already exists");
    }
}
