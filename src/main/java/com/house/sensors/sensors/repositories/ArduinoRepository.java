package com.house.sensors.sensors.repositories;

import com.house.sensors.sensors.entities.Arduino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArduinoRepository extends JpaRepository<Arduino, Long> {
    List<Arduino> findByIsActiveTrue();
    boolean existsByHostName(String hostName);
}
