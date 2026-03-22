package com.house.sensors.sensors.mappers;

import com.house.sensors.sensors.entities.Arduino;
import com.house.sensors.sensors.models.ArduinoDto;
import org.springframework.stereotype.Component;

@Component
public class ArduinoMapper {

    public ArduinoDto toDto(Arduino entity) {
        if (entity == null) {
            return null;
        }
        return ArduinoDto.builder()
                .id(entity.getId())
                .hostName(entity.getHostName())
                .isActive(entity.getIsActive())
                .creationDate(entity.getCreationDate())
                .build();
    }

    public Arduino toEntity(ArduinoDto dto) {
        if (dto == null) {
            return null;
        }
        Arduino entity = new Arduino();
        entity.setHostName(dto.getHostName());
        entity.setIsActive(dto.getIsActive());
        return entity;
    }
}
