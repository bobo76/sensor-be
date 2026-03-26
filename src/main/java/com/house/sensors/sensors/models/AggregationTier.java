package com.house.sensors.sensors.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AggregationTier {
    RAW(null),
    HOURLY("DATE_TRUNC('hour', creation_date)"),
    SIX_HOURLY("TIMESTAMP '1970-01-01' + FLOOR(EXTRACT(EPOCH FROM "
        + "creation_date) / 21600) * 21600 * INTERVAL '1 second'"),
    DAILY("DATE_TRUNC('day', creation_date)"),
    WEEKLY("DATE_TRUNC('week', creation_date)"),
    MONTHLY("DATE_TRUNC('month', creation_date)");

    private final String bucketExpression;
}
