package com.house.sensors.sensors.controllers;

import com.house.sensors.sensors.entities.EntryPointEvent;
import com.house.sensors.sensors.entities.EntryPointEventType;
import com.house.sensors.sensors.mappers.EntryPointEventMapper;
import com.house.sensors.sensors.models.EntryPointEventDto;
import com.house.sensors.sensors.models.EntryPointEventRequest;
import com.house.sensors.sensors.services.EntryPointEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntryPointEventControllerTest {

    @Mock
    private EntryPointEventService service;

    @Mock
    private EntryPointEventMapper mapper;

    @InjectMocks
    private EntryPointEventController controller;

    private EntryPointEventRequest request;
    private EntryPointEvent saved;
    private EntryPointEventDto dto;

    @BeforeEach
    void setUp() {
        Instant when = Instant.parse("2026-05-19T10:00:00Z");
        request = EntryPointEventRequest.builder()
            .eventType("empty dehumidifier black")
            .eventDate(when)
            .build();
        EntryPointEventType type = EntryPointEventType.builder()
            .id(1L)
            .name("empty dehumidifier black")
            .build();
        saved = EntryPointEvent.builder()
            .id(42L)
            .eventType(type)
            .eventDate(when)
            .build();
        dto = EntryPointEventDto.builder()
            .id(42L)
            .eventType("empty dehumidifier black")
            .eventDate(when)
            .build();
    }

    @Test
    void create_shouldReturnCreated() {
        when(service.record(request)).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(dto);

        ResponseEntity<EntryPointEventDto> response =
            controller.create(request);

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void create_shouldPropagateException_whenUnknownType() {
        when(service.record(request))
            .thenThrow(new IllegalArgumentException("nope"));

        assertThatThrownBy(() -> controller.create(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nope");
    }

    @Test
    void list_shouldMapResults() {
        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant end = Instant.parse("2026-06-01T00:00:00Z");
        when(service.list(start, end))
            .thenReturn(List.of(saved));
        when(mapper.toDto(saved)).thenReturn(dto);

        List<EntryPointEventDto> result =
            controller.list(start, end);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void listTypes_shouldReturnNames() {
        EntryPointEventType t1 = EntryPointEventType.builder()
            .id(1L)
            .name("empty dehumidifier black")
            .build();
        EntryPointEventType t2 = EntryPointEventType.builder()
            .id(2L)
            .name("empty dehumidifier white")
            .build();
        when(service.listTypes()).thenReturn(List.of(t1, t2));

        List<String> result = controller.listTypes();

        assertThat(result).containsExactly(
            "empty dehumidifier black",
            "empty dehumidifier white");
    }
}
