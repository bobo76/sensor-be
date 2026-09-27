package com.house.sensors.sensors.repositories;

import com.house.sensors.sensors.models.AggregatedSensorDataDto;
import com.house.sensors.sensors.models.AggregationTier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregatedSensorDataRepositoryTest {

    private static final Instant START =
        Instant.parse("2026-05-01T00:00:00Z");
    private static final Instant END =
        Instant.parse("2026-05-08T00:00:00Z");

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private AggregatedSensorDataRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AggregatedSensorDataRepository(
            entityManager, ZoneId.of("America/New_York"));
        when(entityManager.createNativeQuery(anyString()))
            .thenReturn(query);
    }

    @Test
    void findAggregated_shouldMapInstantOffsetDateTimeAndTimestamp() {
        Instant bucket = Instant.parse("2026-05-01T04:00:00Z");
        when(query.getResultList()).thenReturn(List.of(
            row(bucket),
            row(OffsetDateTime.ofInstant(bucket, ZoneOffset.UTC)),
            row(Timestamp.from(bucket))));

        List<AggregatedSensorDataDto> result = repository.findAggregated(
            "arduino1", START, END, AggregationTier.DAILY);

        assertThat(result)
            .extracting(AggregatedSensorDataDto::getBucketTimestamp)
            .containsOnly(bucket);
        assertThat(result.getFirst().getAvgTemperature()).isEqualTo(21.5);
        assertThat(result.getFirst().getMinHumidity()).isNull();
        assertThat(result.getFirst().getSampleCount()).isEqualTo(4L);
    }

    @Test
    void findAggregated_shouldBucketInConfiguredTimeZone() {
        when(query.getResultList()).thenReturn(List.of());

        repository.findAggregated(
            "arduino1", START, END, AggregationTier.DAILY);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture());
        assertThat(sql.getValue())
            .contains("(DATE_TRUNC('day', local_ts)) AT TIME ZONE :timeZone")
            .contains("creation_date AT TIME ZONE :timeZone AS local_ts")
            .contains("~ '^-?[0-9]+(\\.[0-9]+)?$'");
        verify(query).setParameter("timeZone", "America/New_York");
    }

    private Object[] row(Object bucket) {
        return new Object[] {
            bucket, 21.5, 20.0, 23.0, null, null, null, 4L, 4L, 0L};
    }
}
