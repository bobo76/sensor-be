package com.house.sensors.sensors.restClients;

import com.house.sensors.sensors.models.SensorData;
import com.house.sensors.sensors.util.HostnameValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpTimeoutException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArduinoClientTest {

    private static final String VALID_HOST = "192.168.1.50";

    private ArduinoClient clientRespondingWith(ResponseCreator response) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
            .expect(requestTo("http://" + VALID_HOST + ":80/data"))
            .andRespond(response);
        return new ArduinoClient(
            builder.build(), new ObjectMapper(), new HostnameValidator());
    }

    private ArduinoClient clientWithBody(String body) {
        return clientRespondingWith(
            withSuccess(body, MediaType.APPLICATION_JSON));
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
    void getSensorData_shouldSanitizeUnquotedInfAndOvf() {
        ArduinoClient client = clientWithBody(
            "{\"temperature\": inf, \"humidity\": ovf}");

        Optional<SensorData> result =
            client.getSensorData(VALID_HOST);

        assertThat(result).isPresent();
        assertThat(result.get().getTemperature()).isEqualTo("inf");
        assertThat(result.get().getHumidity()).isEqualTo("ovf");
    }

    @Test
    void getSensorData_shouldReturnEmpty_whenResponseTimesOut() {
        ArduinoClient client = clientRespondingWith(
            withException(new HttpTimeoutException("request timed out")));

        Optional<SensorData> result =
            client.getSensorData(VALID_HOST);

        assertThat(result).isEmpty();
    }

    @Test
    void getSensorData_shouldReturnEmpty_whenHttpError() {
        ArduinoClient client = clientRespondingWith(
            withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        Optional<SensorData> result =
            client.getSensorData(VALID_HOST);

        assertThat(result).isEmpty();
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
