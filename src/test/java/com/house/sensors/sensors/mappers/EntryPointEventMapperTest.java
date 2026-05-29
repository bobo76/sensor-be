package com.house.sensors.sensors.mappers;

import com.house.sensors.sensors.entities.EntryPointEvent;
import com.house.sensors.sensors.entities.EntryPointEventType;
import com.house.sensors.sensors.models.EntryPointEventDto;
import com.house.sensors.sensors.models.EntryPointEventRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EntryPointEventMapperTest {

    private final EntryPointEventMapper mapper =
        new EntryPointEventMapper();

    @Test
    void toEntity_shouldCopyDateAndType() {
        Instant when = Instant.parse("2026-05-19T10:00:00Z");
        EntryPointEventType type = EntryPointEventType.builder()
            .id(1L)
            .name("empty dehumidifier black")
            .build();
        EntryPointEventRequest request = EntryPointEventRequest
            .builder()
            .eventType("empty dehumidifier black")
            .eventDate(when)
            .build();

        EntryPointEvent entity = mapper.toEntity(request, type);

        assertThat(entity.getEventDate()).isEqualTo(when);
        assertThat(entity.getEventType()).isSameAs(type);
        assertThat(entity.getId()).isNull();
    }

    @Test
    void toDto_shouldFlattenTypeName() {
        Instant when = Instant.parse("2026-05-19T10:00:00Z");
        EntryPointEventType type = EntryPointEventType.builder()
            .id(1L)
            .name("empty dehumidifier white")
            .build();
        EntryPointEvent entity = EntryPointEvent.builder()
            .id(7L)
            .eventType(type)
            .eventDate(when)
            .build();

        EntryPointEventDto dto = mapper.toDto(entity);

        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getEventType())
            .isEqualTo("empty dehumidifier white");
        assertThat(dto.getEventDate()).isEqualTo(when);
    }
}
