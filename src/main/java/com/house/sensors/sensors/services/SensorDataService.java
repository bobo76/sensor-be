package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.repositories.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SensorDataService {

    private final SensorDataRepository sensorDataRepository;

    public SensorData saveSensorData(SensorData sensorData) {
        return sensorDataRepository.save(sensorData);
    }

    @Transactional(readOnly = true)
    public List<SensorData> findHistoricalData(
            String machineName,
            Instant startDate,
            Instant endDate,
            int limit) {
        return sensorDataRepository
            .findByMachineNameAndCreationDateBetweenOrderByCreationDateDesc(
                machineName, startDate, endDate,
                PageRequest.of(0, limit));
    }
}