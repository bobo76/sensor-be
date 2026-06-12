package com.house.sensors.sensors.restClients;

import com.house.sensors.sensors.models.SensorData;
import com.house.sensors.sensors.util.HostnameValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArduinoClientTest {

    private static final String VALID_HOST = "192.168.1.50";

    private ArduinoClient clientWithBody(String body) {
        ExchangeFunction exchange = request -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header("Content-Type",
                    MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
        WebClient webClient = WebClient.builder()
            .exchangeFunction(exchange)
            .build();
        return new ArduinoClient(
            webClient, new ObjectMapper(), new HostnameValidator());
    }

    @Test
    void getSensorData_shouldReturnEmpty_whenInvalidHostname() {
        // Arrange
        HostnameValidator mockValidator =
            mock(HostnameValidator.class);
        ArduinoClient client = new ArduinoClient(
            null, new ObjectMapper(), mockValidator);

        when(mockValidator.validateFormat(anyString()))
            .thenReturn(HostnameValidator.ValidationResult
                .invalid("Invalid hostname"));

        // Act
        Optional<SensorData> result =
            client.getSensorData("localhost");

        // Assert
        assertThat(result).isEmpty();
        verify(mockValidator).validateFormat("localhost");
    }

    @Test
    void getSensorData_shouldParseResponse_andEnrichFields() {
        ArduinoClient client = clientWithBody(
            "{\"temperature\": \"22.5\", \"humidity\": \"45.0\"}");

        Optional<SensorData> result =
            client.getSensorData(VALID_HOST);

        assertThat(result).isPresent();
        assertThat(result.get().getTemperature()).isEqualTo("22.5");
        assertThat(result.get().getHumidity()).isEqualTo("45.0");
        assertThat(result.get().getMachineName())
            .isEqualTo(VALID_HOST);
        assertThat(result.get().getCreationDate()).isNotNull();
    }

    @Test
    void getSensorData_shouldSanitizeUnquotedNan() {
        ArduinoClient client = clientWithBody(
            "{\"temperature\": nan, \"humidity\": 45.0}");

        Optional<SensorData> result =
            client.getSensorData(VALID_HOST);

        assertThat(result).isPresent();
        assertThat(result.get().getTemperature()).isEqualTo("nan");
        assertThat(result.get().getHumidity()).isEqualTo("45.0");
    }

    @Test
    void getSensorData_shouldReturnEmpty_whenBodyIsEmpty() {
        ArduinoClient client = clientWithBody("");

        Optional<SensorData> result =
            client.getSensorData(VALID_HOST);

        assertThat(result).isEmpty();
    }

    @Test
    void getSensorData_shouldReturnEmpty_whenJsonIsMalformed() {
        ArduinoClient client = clientWithBody("{not valid json");

        Optional<SensorData> result =
            client.getSensorData(VALID_HOST);

        assertThat(result).isEmpty();
    }
}
