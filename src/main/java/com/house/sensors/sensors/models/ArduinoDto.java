package com.house.sensors.sensors.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArduinoDto {
    private Long id;

    @NotBlank(message = "Host name is required")
    private String hostName;

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    private Instant creationDate;
}
