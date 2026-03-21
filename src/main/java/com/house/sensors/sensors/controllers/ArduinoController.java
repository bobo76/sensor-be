package com.house.sensors.sensors.controllers;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.repositories.ArduinoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Arduino Management", description = "APIs for managing Arduino devices")
@Validated
@RequiredArgsConstructor
@RestController()
@RequestMapping("/arduino")
public class ArduinoController {
    private final ArduinoRepository repository;

    @Operation(summary = "Get all Arduino devices", description = "Retrieves a list of all registered Arduino devices")
    @GetMapping("/")
    public List<Arduino> getArduinos() {
        return repository.findAll();
    }

    @Operation(summary = "Register new Arduino device", description = "Registers a new Arduino device to the system")
    @PostMapping("/")
    public ResponseEntity<Arduino> addArduino(@RequestBody @Valid Arduino newArduino) {
        if (repository.existsByHostName(newArduino.getHostName())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(repository.save(newArduino));
    }
}
