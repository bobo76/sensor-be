package com.house.sensors.sensors.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupInfoLogger {

    private final BuildProperties buildProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void logBuildInfo() {
        String buildTime = buildProperties.getTime()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));

        log.info("Application: {} v{}", buildProperties.getName(), buildProperties.getVersion());
        log.info("Build time:  {}", buildTime);
    }
}
