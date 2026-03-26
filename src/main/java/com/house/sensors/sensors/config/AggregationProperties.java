package com.house.sensors.sensors.config;

import com.house.sensors.sensors.models.AggregationTier;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "sensor.aggregation")
public class AggregationProperties {

    private List<TierConfig> tiers;

    @Data
    public static class TierConfig {
        private long maxDurationHours;
        private AggregationTier tier;
    }
}
