package com.house.sensors.sensors.repositories;

import com.house.sensors.sensors.entities.EntryPointEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntryPointEventTypeRepository
        extends JpaRepository<EntryPointEventType, Long> {
    Optional<EntryPointEventType> findByName(String name);
}
