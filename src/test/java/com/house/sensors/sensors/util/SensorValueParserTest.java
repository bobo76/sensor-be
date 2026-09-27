package com.house.sensors.sensors.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SensorValueParserTest {

    @Test
    void shouldParsePlainDecimals() {
        assertThat(SensorValueParser.parse("21.5")).isEqualTo(21.5);
        assertThat(SensorValueParser.parse("-3")).isEqualTo(-3.0);
    }

    @Test
    void shouldReturnNull_whenNull() {
        assertThat(SensorValueParser.parse(null)).isNull();
    }

    // Must reject exactly what the aggregated SQL regex rejects
    @ParameterizedTest
    @ValueSource(strings = {
        "nan", "inf", "ovf", "Infinity", "NaN", "1e3", "+21.5",
        " 21.5", "21.", ".5", "", "garbage"})
    void shouldRejectNonPlainDecimals(String value) {
        assertThat(SensorValueParser.parse(value)).isNull();
    }
}
