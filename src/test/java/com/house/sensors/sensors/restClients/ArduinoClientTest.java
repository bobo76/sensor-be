package com.house.sensors.sensors.restClients;

import com.house.sensors.sensors.models.SensorData;
import com.house.sensors.sensors.util.HostnameValidator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArduinoClientTest {

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
}
