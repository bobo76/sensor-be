package com.house.sensors.sensors.repositories;

import com.house.sensors.sensors.models.AggregatedSensorDataDto;
import com.house.sensors.sensors.models.AggregationTier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import com.house.sensors.sensors.util.SensorValueParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Repository
public class AggregatedSensorDataRepository {

    private final EntityManager entityManager;
    private final ZoneId timeZone;

    public AggregatedSensorDataRepository(
            EntityManager entityManager,
            @Value("${sensor.aggregation.time-zone}") ZoneId timeZone) {
        this.entityManager = entityManager;
        this.timeZone = timeZone;
    }

    @SuppressWarnings("unchecked")
    public List<AggregatedSensorDataDto> findAggregated(
            String machineName,
            Instant start,
            Instant end,
            AggregationTier tier) {

        // Parse each reading independently: a row with one broken
        // sensor (or any non-numeric value) still contributes its
        // valid column instead of being dropped entirely.
        // Buckets are computed on house-local wall-clock time so
        // daily/weekly/monthly boundaries fall on local midnight,
        // regardless of the DB session or JVM time zone.
        String sql = """
            SELECT (%1$s) AT TIME ZONE :timeZone AS bucket_timestamp,
              AVG(temp_val) AS avg_temp,
              MIN(temp_val) AS min_temp,
              MAX(temp_val) AS max_temp,
              AVG(hum_val) AS avg_hum,
              MIN(hum_val) AS min_hum,
              MAX(hum_val) AS max_hum,
              COUNT(*) AS sample_count,
              COUNT(temp_val) AS temp_count,
              COUNT(hum_val) AS hum_count
            FROM (
              SELECT creation_date AT TIME ZONE :timeZone AS local_ts,
                CASE WHEN temperature ~ '%2$s' \
            THEN CAST(temperature AS DOUBLE PRECISION) END AS temp_val,
                CASE WHEN humidity ~ '%2$s' \
            THEN CAST(humidity AS DOUBLE PRECISION) END AS hum_val
              FROM sensor_data
              WHERE machine_name = :machineName
                AND creation_date BETWEEN :start AND :end
            ) AS parsed
            WHERE temp_val IS NOT NULL OR hum_val IS NOT NULL
            GROUP BY bucket_timestamp
            ORDER BY bucket_timestamp ASC
            """.formatted(tier.getBucketExpression(),
                SensorValueParser.NUMERIC_REGEX);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("machineName", machineName);
        query.setParameter("start", start);
        query.setParameter("end", end);
        query.setParameter("timeZone", timeZone.getId());

        List<Object[]> rows = query.getResultList();
        return rows.stream()
            .map(row -> mapRow(row, machineName))
            .toList();
    }

    private AggregatedSensorDataDto mapRow(Object[] row,
                                           String machineName) {
        return AggregatedSensorDataDto.builder()
            .bucketTimestamp(toInstant(row[0]))
            .machineName(machineName)
            .avgTemperature(toDouble(row[1]))
            .minTemperature(toDouble(row[2]))
            .maxTemperature(toDouble(row[3]))
            .avgHumidity(toDouble(row[4]))
            .minHumidity(toDouble(row[5]))
            .maxHumidity(toDouble(row[6]))
            .sampleCount(((Number) row[7]).longValue())
            .temperatureSampleCount(((Number) row[8]).longValue())
            .humiditySampleCount(((Number) row[9]).longValue())
            .build();
    }

    // Hibernate 7 maps timestamptz to Instant/OffsetDateTime; accept
    // Timestamp too in case the dialect or driver config changes.
    private Instant toInstant(Object value) {
        return switch (value) {
            case Instant instant -> instant;
            case OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
            case Timestamp timestamp -> timestamp.toInstant();
            default -> throw new IllegalStateException(
                "Unexpected bucket_timestamp type: " + value.getClass());
        };
    }

    private Double toDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }
}
