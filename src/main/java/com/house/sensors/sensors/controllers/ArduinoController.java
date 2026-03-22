package com.house.sensors.sensors.controllers;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.mappers.ArduinoMapper;
import com.house.sensors.sensors.models.ArduinoDto;
import com.house.sensors.sensors.services.ArduinoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Arduino Management",
    description = "APIs for managing Arduino devices")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/arduino")
public class ArduinoController {
    private final ArduinoService arduinoService;
    private final ArduinoMapper arduinoMapper;

    @Operation(summary = "Get all Arduino devices",
        description = "Retrieves a list of all registered "
            + "Arduino devices")
    @GetMapping("/")
    public List<ArduinoDto> getArduinos() {
        return arduinoService.findAll().stream()
            .map(arduinoMapper::toDto)
            .toList();
    }

    @Operation(summary = "Register new Arduino device",
        description = "Registers a new Arduino device "
            + "to the system")
    @PostMapping("/")
    public ResponseEntity<ArduinoDto> addArduino(
            @RequestBody @Valid ArduinoDto request) {
        Arduino entity = arduinoMapper.toEntity(request);
        return arduinoService.addArduino(entity)
            .map(arduinoMapper::toDto)
            .map(dto -> ResponseEntity.status(HttpStatus.CREATED)
                .body(dto))
            .orElseGet(
                () -> ResponseEntity.badRequest().build());
    }
}
