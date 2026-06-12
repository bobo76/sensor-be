package com.house.sensors.sensors.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AggregationTier {
    RAW(null),
    HOURLY("DATE_TRUNC('hour', creation_date)"),
    SIX_HOURLY("TO_TIMESTAMP(FLOOR(EXTRACT(EPOCH FROM "
        + "creation_date) / 21600) * 21600)"),
    DAILY("DATE_TRUNC('day', creation_date)"),
    WEEKLY("DATE_TRUNC('week', creation_date)"),
    MONTHLY("DATE_TRUNC('month', creation_date)");

    private final String bucketExpression;
}
