package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.EntryPointEvent;
import com.house.sensors.sensors.entities.EntryPointEventType;
import com.house.sensors.sensors.mappers.EntryPointEventMapper;
import com.house.sensors.sensors.models.EntryPointEventRequest;
import com.house.sensors.sensors.repositories.EntryPointEventRepository;
import com.house.sensors.sensors.repositories.EntryPointEventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntryPointEventService {

    private final EntryPointEventRepository eventRepository;
    private final EntryPointEventTypeRepository typeRepository;
    private final EntryPointEventMapper mapper;

    public EntryPointEvent record(EntryPointEventRequest request) {
        EntryPointEventType type = typeRepository
            .findByName(request.getEventType())
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown event type: " + request.getEventType()));
        return eventRepository.save(mapper.toEntity(request, type));
    }

    public List<EntryPointEvent> list(Instant start, Instant end) {
        Instant effectiveEnd = end != null ? end : Instant.now();
        Instant effectiveStart = start != null
            ? start
            : effectiveEnd.minus(30, ChronoUnit.DAYS);
        return eventRepository
            .findByEventDateBetweenOrderByEventDateDesc(
                effectiveStart, effectiveEnd);
    }

    public List<EntryPointEventType> listTypes() {
        return typeRepository.findAll();
    }
}
