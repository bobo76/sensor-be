package com.house.sensors.sensors.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(indexes = {
    @Index(name = "idx_hostname", columnList = "hostName")
})
public class Arduino {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Host name is required")
    @Column(unique = true)
    private String hostName;

    @NotNull(message = "Active status is required")
    @Column(nullable = false)
    private Boolean isActive;

    @Column(columnDefinition = "TIMESTAMPTZ(6) DEFAULT NOW()", insertable = false, updatable = false)
    private Instant creationDate;
}
