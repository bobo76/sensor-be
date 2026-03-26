package com.house.sensors.sensors.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedDataResponse {
    private AggregationTier aggregationTier;
    private List<AggregatedSensorDataDto> data;
}
