package com.house.sensors.sensors.repositories;

import com.house.sensors.sensors.models.AggregatedSensorDataDto;
import com.house.sensors.sensors.models.AggregationTier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AggregatedSensorDataRepository {

    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<AggregatedSensorDataDto> findAggregated(
            String machineName,
            Instant start,
            Instant end,
            AggregationTier tier) {

        // Parse each reading independently: a row with one broken
        // sensor (or any non-numeric value) still contributes its
        // valid column instead of being dropped entirely.
        String sql = """
            SELECT %s AS bucket_timestamp,
              AVG(temp_val) AS avg_temp,
              MIN(temp_val) AS min_temp,
              MAX(temp_val) AS max_temp,
              AVG(hum_val) AS avg_hum,
              MIN(hum_val) AS min_hum,
              MAX(hum_val) AS max_hum,
              COUNT(*) AS sample_count
            FROM (
              SELECT creation_date,
                CASE WHEN temperature ~ '^-?[0-9]+(\\.[0-9]+)?$' \
            THEN CAST(temperature AS DOUBLE PRECISION) END AS temp_val,
                CASE WHEN humidity ~ '^-?[0-9]+(\\.[0-9]+)?$' \
            THEN CAST(humidity AS DOUBLE PRECISION) END AS hum_val
              FROM sensor_data
              WHERE machine_name = :machineName
                AND creation_date BETWEEN :start AND :end
            ) AS parsed
            WHERE temp_val IS NOT NULL OR hum_val IS NOT NULL
            GROUP BY bucket_timestamp
            ORDER BY bucket_timestamp ASC
            """.formatted(tier.getBucketExpression());

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("machineName", machineName);
        query.setParameter("start", start);
        query.setParameter("end", end);

        List<Object[]> rows = query.getResultList();
        return rows.stream()
            .map(row -> mapRow(row, machineName))
            .toList();
    }

    private AggregatedSensorDataDto mapRow(Object[] row,
                                           String machineName) {
        return AggregatedSensorDataDto.builder()
            .bucketTimestamp(
                ((Timestamp) row[0]).toInstant())
            .machineName(machineName)
            .avgTemperature(toDouble(row[1]))
            .minTemperature(toDouble(row[2]))
            .maxTemperature(toDouble(row[3]))
            .avgHumidity(toDouble(row[4]))
            .minHumidity(toDouble(row[5]))
            .maxHumidity(toDouble(row[6]))
            .sampleCount(((Number) row[7]).longValue())
            .build();
    }

    private Double toDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }
}
