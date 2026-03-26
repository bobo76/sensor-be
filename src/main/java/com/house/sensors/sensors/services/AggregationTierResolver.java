package com.house.sensors.sensors.services;

import com.house.sensors.sensors.config.AggregationProperties;
import com.house.sensors.sensors.models.AggregationTier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AggregationTierResolver {

    private final AggregationProperties properties;

    @PostConstruct
    void validate() {
        List<AggregationProperties.TierConfig> tiers =
            properties.getTiers();
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalStateException(
                "sensor.aggregation.tiers must not be empty");
        }
        for (int i = 1; i < tiers.size(); i++) {
            if (tiers.get(i).getMaxDurationHours()
                    <= tiers.get(i - 1).getMaxDurationHours()) {
                throw new IllegalStateException(
                    "sensor.aggregation.tiers must be sorted "
                        + "ascending by maxDurationHours");
            }
        }
    }

    public AggregationTier resolve(Instant startDate,
                                   Instant endDate) {
        long hours = Duration.between(startDate, endDate).toHours();
        for (AggregationProperties.TierConfig tier
                : properties.getTiers()) {
            if (hours <= tier.getMaxDurationHours()) {
                return tier.getTier();
            }
        }
        return properties.getTiers()
            .getLast().getTier();
    }
}
