package com.house.sensors.sensors.mappers;

import com.house.sensors.sensors.entities.EntryPointEvent;
import com.house.sensors.sensors.entities.EntryPointEventType;
import com.house.sensors.sensors.models.EntryPointEventDto;
import com.house.sensors.sensors.models.EntryPointEventRequest;
import org.springframework.stereotype.Component;

@Component
public class EntryPointEventMapper {

    public EntryPointEvent toEntity(
            EntryPointEventRequest request,
            EntryPointEventType type) {
        return EntryPointEvent.builder()
            .eventType(type)
            .eventDate(request.getEventDate())
            .build();
    }

    public EntryPointEventDto toDto(EntryPointEvent entity) {
        return EntryPointEventDto.builder()
            .id(entity.getId())
            .eventType(entity.getEventType().getName())
            .eventDate(entity.getEventDate())
            .build();
    }
}
