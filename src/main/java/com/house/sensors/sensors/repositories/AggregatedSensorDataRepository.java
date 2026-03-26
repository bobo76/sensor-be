package com.house.sensors.sensors.repositories;

import com.house.sensors.sensors.models.AggregatedSensorDataDto;
import com.house.sensors.sensors.models.AggregationTier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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

        String sql = """
            SELECT %s AS bucket_timestamp,
              AVG(CAST(temperature AS DOUBLE PRECISION)) \
            AS avg_temp,
              MIN(CAST(temperature AS DOUBLE PRECISION)) \
            AS min_temp,
              MAX(CAST(temperature AS DOUBLE PRECISION)) \
            AS max_temp,
              AVG(CAST(humidity AS DOUBLE PRECISION)) \
            AS avg_hum,
              MIN(CAST(humidity AS DOUBLE PRECISION)) \
            AS min_hum,
              MAX(CAST(humidity AS DOUBLE PRECISION)) \
            AS max_hum,
              COUNT(*) AS sample_count
            FROM sensor_data
            WHERE machine_name = :machineName
              AND creation_date BETWEEN :start AND :end
              AND LOWER(temperature) != 'nan'
              AND LOWER(humidity) != 'nan'
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
                ((java.sql.Timestamp) row[0]).toInstant())
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
