package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.mappers.SensorDataMapper;
import com.house.sensors.sensors.repositories.ArduinoRepository;
import com.house.sensors.sensors.restClients.ArduinoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorScheduledServices {

    private final ArduinoClient client;
    private final ArduinoRepository repository;
    private final SensorDataService sensorDataService;
    private final SensorDataMapper sensorDataMapper;

    @Value("${sensor.polling.log-interval-hours:6}")
    private int logIntervalHours;

    private volatile Instant lastLogTime = Instant.MIN;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info(
            "SensorScheduledServices is starting, "
                + "scheduled every 15 minutes (00, 15, 30, 45), "
                + "logging every {} hours",
            logIntervalHours);
    }

    @Scheduled(cron = "0 0,15,30,45 * * * *")
    public void runTask() {
        List<Arduino> arduinos = repository.findByIsActiveTrue();

        long successCount;
        try (ExecutorService executor =
                 Executors.newVirtualThreadPerTaskExecutor()) {
            successCount = arduinos.stream()
                .map(arduino -> executor.submit(
                    () -> pollAndSaveArduinoData(arduino)))
                .toList()
                .stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return Optional.<SensorData>empty();
                    }
                })
                .filter(Optional::isPresent)
                .count();
        }

        Instant now = Instant.now();
        if (ChronoUnit.HOURS.between(lastLogTime, now)
            >= logIntervalHours) {
            log.info("Sensor polling complete: {}/{} successful",
                successCount, arduinos.size());
            lastLogTime = now;
        }
    }

    private Optional<SensorData> pollAndSaveArduinoData(
            Arduino arduino) {
        try {
            return client.getSensorData(arduino.getHostName())
                .map(sensorDataMapper::toSensorDataEntity)
                .map(sensorDataService::saveSensorData);
        } catch (Exception e) {
            log.error("Failed to poll Arduino '{}': {}",
                arduino.getHostName(), e.getMessage());
            return Optional.empty();
        }
    }
}
