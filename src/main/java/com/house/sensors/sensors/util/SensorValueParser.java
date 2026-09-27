package com.house.sensors.sensors.util;

import java.util.regex.Pattern;

/**
 * Single definition of what counts as a numeric sensor reading, shared by
 * the Java RAW tier and the SQL aggregated tiers so both accept the same
 * rows.
 */
public final class SensorValueParser {

    /** Plain decimal, valid as both a Java and a PostgreSQL POSIX regex. */
    public static final String NUMERIC_REGEX = "^-?[0-9]+(\\.[0-9]+)?$";

    private static final Pattern NUMERIC_PATTERN =
        Pattern.compile(NUMERIC_REGEX);

    private SensorValueParser() {
    }

    public static Double parse(String value) {
        if (value == null || !NUMERIC_PATTERN.matcher(value).matches()) {
            return null;
        }
        return Double.parseDouble(value);
    }
}
