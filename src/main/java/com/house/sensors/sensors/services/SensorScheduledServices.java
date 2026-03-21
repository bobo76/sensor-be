package com.house.sensors.sensors.services;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.entities.SensorData;
import com.house.sensors.sensors.mappers.SensorDataMapper;
import com.house.sensors.sensors.repositories.ArduinoRepository;
import com.house.sensors.sensors.restClients.ArduinoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SensorScheduledServices {
    private static final Logger logger = LoggerFactory.getLogger(SensorScheduledServices.class);

    private final ArduinoClient client;
    private final ArduinoRepository repository;
    private final SensorDataService sensorDataService;
    private final SensorDataMapper sensorDataMapper;

    @Value("${sensor.polling.log-interval-hours:6}")
    private int logIntervalHours;

    private Instant lastLogTime = Instant.MIN;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("SensorScheduledServices is starting, scheduled every 15 minutes (00, 15, 30, 45), logging every {} hours", logIntervalHours);
    }

    @Scheduled(cron = "0 0,15,30,45 * * * *")  // Runs at 00, 15, 30, 45 minutes past every hour
    public void runTask() {
        var arduinos = repository.findByIsActiveTrue();

        long successCount = arduinos.parallelStream()
                .map(this::pollAndSaveArduinoData)
                .filter(Optional::isPresent)
                .count();

        Instant now = Instant.now();
        if (ChronoUnit.HOURS.between(lastLogTime, now) >= logIntervalHours) {
            logger.info("Sensor polling complete: {}/{} successful", successCount, arduinos.size());
            lastLogTime = now;
        }
    }

    private Optional<SensorData> pollAndSaveArduinoData(Arduino arduino) {
        try {
            return client.getSensorData(arduino.getHostName())
                    .map(sensorDataMapper::toSensorDataEntity)
                    .map(sensorDataService::saveSensorData);
        } catch (Exception e) {
            logger.error("Failed to poll Arduino '{}': {}", arduino.getHostName(), e.getMessage());
            return Optional.empty();
        }
    }
}
