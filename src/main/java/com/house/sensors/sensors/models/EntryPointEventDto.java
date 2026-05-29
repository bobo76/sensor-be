package com.house.sensors.sensors.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntryPointEventDto {
    private Long id;
    private String eventType;
    private Instant eventDate;
}
