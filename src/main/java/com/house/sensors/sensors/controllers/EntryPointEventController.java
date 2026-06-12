package com.house.sensors.sensors.controllers;

import com.house.sensors.sensors.entities.EntryPointEvent;
import com.house.sensors.sensors.entities.EntryPointEventType;
import com.house.sensors.sensors.mappers.EntryPointEventMapper;
import com.house.sensors.sensors.models.EntryPointEventDto;
import com.house.sensors.sensors.models.EntryPointEventRequest;
import com.house.sensors.sensors.services.EntryPointEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Tag(name = "Entry Point Events",
    description = "APIs for recording and listing "
        + "entry point events (e.g. dehumidifier emptied)")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/entry-point-events")
public class EntryPointEventController {

    private final EntryPointEventService service;
    private final EntryPointEventMapper mapper;

    @Operation(summary = "Record an entry point event")
    @PostMapping("/")
    public ResponseEntity<EntryPointEventDto> create(
            @RequestBody @Valid EntryPointEventRequest request) {
        EntryPointEvent saved = service.record(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapper.toDto(saved));
    }

    @Operation(summary = "List entry point events",
        description = "Returns events between startDate and "
            + "endDate (defaults to last 30 days)")
    @GetMapping("/")
    public List<EntryPointEventDto> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant endDate) {
        return service.list(startDate, endDate).stream()
            .map(mapper::toDto)
            .toList();
    }

    @Operation(summary = "List available event types")
    @GetMapping("/types")
    public List<String> listTypes() {
        return service.listTypes().stream()
            .map(EntryPointEventType::getName)
            .toList();
    }
}
