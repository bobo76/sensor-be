package com.house.sensors.sensors.repositories;

import com.house.sensors.sensors.entities.EntryPointEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EntryPointEventRepository
        extends JpaRepository<EntryPointEvent, Long> {
    List<EntryPointEvent>
        findByEventDateBetweenOrderByEventDateDesc(
            Instant start, Instant end);
}
