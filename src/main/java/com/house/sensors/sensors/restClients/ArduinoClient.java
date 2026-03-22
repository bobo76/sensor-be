package com.house.sensors.sensors.restClients;

import com.house.sensors.sensors.models.SensorData;
import com.house.sensors.sensors.util.HostnameValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class ArduinoClient {
    private static final Pattern NAN_PATTERN = Pattern.compile(":\\s*(?i)(nan)\\b");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient client;
    private final ObjectMapper objectMapper;
    private final HostnameValidator hostnameValidator;

    public Optional<SensorData> getSensorData(String machineName) {
        if (!validateHostname(machineName)) {
            return Optional.empty();
        }

        try {
            String responseBody = fetchRawDataFromArduino(machineName);
            return mapResponseToSensorData(responseBody, machineName);
        } catch (WebClientRequestException ex) {
            handleNetworkError(ex, machineName);
        } catch (WebClientResponseException ex) {
            handleHttpError(ex, machineName);
        } catch (JacksonException ex) {
            handleParsingError(ex, machineName);
        } catch (Exception ex) {
            handleUnexpectedError(ex, machineName);
        }
        return Optional.empty();
    }

    private boolean validateHostname(String machineName) {
        HostnameValidator.ValidationResult result =
            hostnameValidator.validate(machineName);
        if (!result.isValid()) {
            log.error("Invalid hostname '{}': {}",
                machineName, result.errorMessage());
            return false;
        }
        return true;
    }

    private String fetchRawDataFromArduino(String machineName) {
        String response = client.get()
            .uri("http://" + machineName + ":80/data")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(REQUEST_TIMEOUT)
            .block();

        if (response == null) {
            log.warn("Null response from Arduino device '{}'",
                machineName);
        }

        return response;
    }

    private Optional<SensorData> mapResponseToSensorData(
            String responseBody, String machineName) {
        if (responseBody == null || responseBody.isEmpty()) {
            log.warn("Empty response from Arduino device '{}'",
                machineName);
            return Optional.empty();
        }

        String sanitizedJson = sanitizeJsonResponse(responseBody);
        SensorData sensorData =
            parseJsonToSensorData(sanitizedJson);
        enrichSensorData(sensorData, machineName);

        log.debug(
            "Successfully fetched sensor data from Arduino "
                + "device '{}'", machineName);
        return Optional.of(sensorData);
    }

    private String sanitizeJsonResponse(String responseBody) {
        return NAN_PATTERN.matcher(responseBody)
            .replaceAll(": \"nan\"");
    }

    private SensorData parseJsonToSensorData(String json) {
        return objectMapper.readValue(json, SensorData.class);
    }

    private void enrichSensorData(
            SensorData sensorData, String machineName) {
        sensorData.setMachineName(machineName);
        sensorData.setCreationDate(Instant.now());
    }

    private void handleNetworkError(
            WebClientRequestException ex, String machineName) {
        Throwable cause = ex.getCause();
        if (cause instanceof UnknownHostException) {
            log.error("Cannot resolve hostname for Arduino "
                + "device '{}': Device may be offline or "
                + "hostname is incorrect", machineName);
        } else if (cause instanceof TimeoutException) {
            log.error("Timeout connecting to Arduino device "
                + "'{}': Device not responding", machineName);
        } else {
            log.error("Network error connecting to Arduino "
                + "device '{}': {}",
                machineName, ex.getMessage());
        }
    }

    private void handleHttpError(
            WebClientResponseException ex,
            String machineName) {
        log.error("HTTP error from Arduino device '{}': "
            + "{} - {}", machineName,
            ex.getStatusCode(), ex.getMessage());
    }

    private void handleParsingError(
            JacksonException ex, String machineName) {
        log.error("Failed to parse JSON response from "
            + "Arduino device '{}': {}",
            machineName, ex.getMessage());
    }

    private void handleUnexpectedError(
            Exception ex, String machineName) {
        Throwable cause = ex.getCause();
        if (cause instanceof TimeoutException) {
            log.error("Timeout fetching data from Arduino "
                + "device '{}': Device not responding "
                + "within {}s", machineName,
                REQUEST_TIMEOUT.getSeconds());
        } else {
            log.error("Unexpected error (type: {}) fetching "
                + "data from Arduino device '{}': {}",
                ex.getClass().getSimpleName(),
                machineName, ex.getMessage(), ex);
        }
    }
}
