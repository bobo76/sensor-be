package com.house.sensors.sensors.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@Builder
@Data
@Entity
@NoArgsConstructor
@Table(indexes = {
    @Index(name = "idx_machine_date", columnList = "machineName, creationDate")
})
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String machineName;
    private Instant creationDate;
    private String temperature;
    private String humidity;
}
