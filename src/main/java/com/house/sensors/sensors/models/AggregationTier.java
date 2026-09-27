package com.house.sensors.sensors.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AggregationTier {
    RAW(null),
    HOURLY("DATE_TRUNC('hour', local_ts)"),
    SIX_HOURLY("TO_TIMESTAMP(FLOOR(EXTRACT(EPOCH FROM "
        + "local_ts) / 21600) * 21600) AT TIME ZONE 'UTC'"),
    DAILY("DATE_TRUNC('day', local_ts)"),
    WEEKLY("DATE_TRUNC('week', local_ts)"),
    MONTHLY("DATE_TRUNC('month', local_ts)");

    /**
     * Operates on {@code local_ts} (a timestamp without time zone in the
     * configured house zone) so buckets align with local days/weeks.
     */
    private final String bucketExpression;
}
