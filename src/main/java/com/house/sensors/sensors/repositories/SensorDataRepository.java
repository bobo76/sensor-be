package com.house.sensors.sensors.repositories;

import com.house.sensors.sensors.entities.SensorData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
    List<SensorData> findByMachineNameAndCreationDateBetween(
            String machineName, Instant start, Instant end, Pageable pageable);
}
