package com.house.sensors.sensors.restClients;

import com.house.sensors.sensors.models.SensorData;
import com.house.sensors.sensors.util.HostnameValidator;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
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
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class ArduinoClient {
    // Arduino's Print::printFloat emits bare nan/inf/ovf tokens, which
    // are not valid JSON; quote them so the reading still parses.
    private static final Pattern NON_FINITE_PATTERN =
        Pattern.compile(":\\s*(?i)(nan|-?inf|ovf)\\b");

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
            hostnameValidator.validateFormat(machineName);
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
        return NON_FINITE_PATTERN.matcher(responseBody)
            .replaceAll(": \"$1\"");
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
        } else if (cause instanceof ConnectTimeoutException
                || cause instanceof ReadTimeoutException) {
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
        log.error("Unexpected error (type: {}) fetching "
            + "data from Arduino device '{}': {}",
            ex.getClass().getSimpleName(),
            machineName, ex.getMessage(), ex);
    }
}
