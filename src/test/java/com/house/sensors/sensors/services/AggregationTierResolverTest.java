package com.house.sensors.sensors.services;

import com.house.sensors.sensors.config.AggregationProperties;
import com.house.sensors.sensors.models.AggregationTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregationTierResolverTest {

    private AggregationTierResolver resolver;

    @BeforeEach
    void setUp() {
        AggregationProperties props = new AggregationProperties();
        props.setTiers(List.of(
            tier(24, AggregationTier.RAW),
            tier(168, AggregationTier.HOURLY),
            tier(720, AggregationTier.SIX_HOURLY),
            tier(4380, AggregationTier.DAILY),
            tier(8760, AggregationTier.WEEKLY),
            tier(Integer.MAX_VALUE, AggregationTier.MONTHLY)
        ));
        resolver = new AggregationTierResolver(props);
        resolver.validate();
    }

    @Test
    void resolve_shouldReturnRaw_whenUnder24Hours() {
        Instant start = Instant.now();
        Instant end = start.plus(12, ChronoUnit.HOURS);
        assertThat(resolver.resolve(start, end))
            .isEqualTo(AggregationTier.RAW);
    }

    @Test
    void resolve_shouldReturnRaw_whenExactly24Hours() {
        Instant start = Instant.now();
        Instant end = start.plus(24, ChronoUnit.HOURS);
        assertThat(resolver.resolve(start, end))
            .isEqualTo(AggregationTier.RAW);
    }

    @Test
    void resolve_shouldReturnHourly_whenBetween1DayAnd1Week() {
        Instant start = Instant.now();
        Instant end = start.plus(3, ChronoUnit.DAYS);
        assertThat(resolver.resolve(start, end))
            .isEqualTo(AggregationTier.HOURLY);
    }

    @Test
    void resolve_shouldReturnSixHourly_whenBetween1WeekAnd1Month() {
        Instant start = Instant.now();
        Instant end = start.plus(14, ChronoUnit.DAYS);
        assertThat(resolver.resolve(start, end))
            .isEqualTo(AggregationTier.SIX_HOURLY);
    }

    @Test
    void resolve_shouldReturnDaily_whenBetween1MonthAnd6Months() {
        Instant start = Instant.now();
        Instant end = start.plus(90, ChronoUnit.DAYS);
        assertThat(resolver.resolve(start, end))
            .isEqualTo(AggregationTier.DAILY);
    }

    @Test
    void resolve_shouldReturnWeekly_whenBetween6MonthsAnd1Year() {
        Instant start = Instant.now();
        Instant end = start.plus(300, ChronoUnit.DAYS);
        assertThat(resolver.resolve(start, end))
            .isEqualTo(AggregationTier.WEEKLY);
    }

    @Test
    void resolve_shouldReturnMonthly_whenOver1Year() {
        Instant start = Instant.now();
        Instant end = start.plus(500, ChronoUnit.DAYS);
        assertThat(resolver.resolve(start, end))
            .isEqualTo(AggregationTier.MONTHLY);
    }

    @Test
    void validate_shouldThrow_whenTiersEmpty() {
        AggregationProperties props = new AggregationProperties();
        props.setTiers(List.of());
        AggregationTierResolver badResolver =
            new AggregationTierResolver(props);

        assertThatThrownBy(badResolver::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must not be empty");
    }

    @Test
    void validate_shouldThrow_whenTiersNotSorted() {
        AggregationProperties props = new AggregationProperties();
        props.setTiers(List.of(
            tier(168, AggregationTier.HOURLY),
            tier(24, AggregationTier.RAW)
        ));
        AggregationTierResolver badResolver =
            new AggregationTierResolver(props);

        assertThatThrownBy(badResolver::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("sorted ascending");
    }

    private static AggregationProperties.TierConfig tier(
            long maxHours, AggregationTier aggregationTier) {
        AggregationProperties.TierConfig config =
            new AggregationProperties.TierConfig();
        config.setMaxDurationHours(maxHours);
        config.setTier(aggregationTier);
        return config;
    }
}
