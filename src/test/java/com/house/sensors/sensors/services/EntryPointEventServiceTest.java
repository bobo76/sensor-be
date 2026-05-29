package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.EntryPointEvent;
import com.house.sensors.sensors.entities.EntryPointEventType;
import com.house.sensors.sensors.mappers.EntryPointEventMapper;
import com.house.sensors.sensors.models.EntryPointEventRequest;
import com.house.sensors.sensors.repositories.EntryPointEventRepository;
import com.house.sensors.sensors.repositories.EntryPointEventTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntryPointEventServiceTest {

    @Mock
    private EntryPointEventRepository eventRepository;

    @Mock
    private EntryPointEventTypeRepository typeRepository;

    @Mock
    private EntryPointEventMapper mapper;

    @InjectMocks
    private EntryPointEventService service;

    private EntryPointEventType blackType;

    @BeforeEach
    void setUp() {
        blackType = EntryPointEventType.builder()
            .id(1L)
            .name("empty dehumidifier black")
            .build();
    }

    @Test
    void record_shouldSaveEvent_whenTypeExists() {
        Instant when = Instant.parse("2026-05-19T10:00:00Z");
        EntryPointEventRequest request = EntryPointEventRequest
            .builder()
            .eventType("empty dehumidifier black")
            .eventDate(when)
            .build();
        EntryPointEvent mapped = EntryPointEvent.builder()
            .eventType(blackType)
            .eventDate(when)
            .build();
        EntryPointEvent saved = EntryPointEvent.builder()
            .id(42L)
            .eventType(blackType)
            .eventDate(when)
            .build();

        when(typeRepository.findByName(
                "empty dehumidifier black"))
            .thenReturn(Optional.of(blackType));
        when(mapper.toEntity(request, blackType))
            .thenReturn(mapped);
        when(eventRepository.save(mapped)).thenReturn(saved);

        EntryPointEvent result = service.record(request);

        assertThat(result.getId()).isEqualTo(42L);
        verify(eventRepository).save(mapped);
    }

    @Test
    void record_shouldThrow_whenTypeUnknown() {
        EntryPointEventRequest request = EntryPointEventRequest
            .builder()
            .eventType("bogus")
            .eventDate(Instant.now())
            .build();
        when(typeRepository.findByName("bogus"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.record(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bogus");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void list_shouldUseProvidedRange() {
        Instant start = Instant.parse("2026-05-01T00:00:00Z");
        Instant end = Instant.parse("2026-06-01T00:00:00Z");
        when(eventRepository
                .findByEventDateBetweenOrderByEventDateDesc(
                    start, end))
            .thenReturn(List.of());

        service.list(start, end);

        verify(eventRepository)
            .findByEventDateBetweenOrderByEventDateDesc(start, end);
    }

    @Test
    void list_shouldDefaultToLast30Days_whenNullParams() {
        ArgumentCaptor<Instant> startCap =
            ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> endCap =
            ArgumentCaptor.forClass(Instant.class);
        when(eventRepository
                .findByEventDateBetweenOrderByEventDateDesc(
                    any(), any()))
            .thenReturn(List.of());

        Instant before = Instant.now();
        service.list(null, null);
        Instant after = Instant.now();

        verify(eventRepository)
            .findByEventDateBetweenOrderByEventDateDesc(
                startCap.capture(), endCap.capture());

        assertThat(endCap.getValue())
            .isBetween(before, after);
        assertThat(startCap.getValue())
            .isBetween(
                before.minus(30, ChronoUnit.DAYS).minusSeconds(1),
                after.minus(30, ChronoUnit.DAYS).plusSeconds(1));
    }

    @Test
    void listTypes_shouldDelegateToRepository() {
        when(typeRepository.findAll())
            .thenReturn(List.of(blackType));

        List<EntryPointEventType> result = service.listTypes();

        assertThat(result).containsExactly(blackType);
    }
}
