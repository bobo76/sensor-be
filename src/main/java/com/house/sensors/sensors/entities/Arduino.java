package com.house.sensors.sensors.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(indexes = {
    @Index(name = "idx_hostname", columnList = "hostName")
})
public class Arduino {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @NotBlank(message = "Host name is required")
    @Column(unique = true)
    private String hostName;

    @NotNull(message = "Active status is required")
    @Column(nullable = false)
    private Boolean isActive;

    @Column(columnDefinition = "TIMESTAMPTZ(6) DEFAULT NOW()",
        insertable = false, updatable = false)
    private Instant creationDate;
}
