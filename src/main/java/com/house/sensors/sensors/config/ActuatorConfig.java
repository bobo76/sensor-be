package com.house.sensors.sensors.config;

import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

@Configuration
public class ActuatorConfig {

    /**
     * Lists application/json first so clients without a specific Accept header
     * get plain JSON instead of the actuator vendor media type.
     */
    @Bean
    public EndpointMediaTypes endpointMediaTypes() {
        return new EndpointMediaTypes(
                MediaType.APPLICATION_JSON_VALUE,
                "application/vnd.spring-boot.actuator.v3+json",
                "application/vnd.spring-boot.actuator.v2+json");
    }
}
