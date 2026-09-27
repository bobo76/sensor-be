package com.house.sensors.sensors.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Component
public class HostnameValidator {

    // DNS hostname pattern (RFC 1123)
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)*"
            + "[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$"
    );

    // IPv4 pattern
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // Blocked patterns for security
    private static final Pattern BLOCKED_PATTERN = Pattern.compile(
        "^(localhost|127\\..*|0\\..*"
            + "|metadata\\..*|169\\.254\\..*)$",
        Pattern.CASE_INSENSITIVE
    );

    // An all-digit last label is not a valid DNS name (RFC 3696 §2);
    // resolvers treat it as a shorthand IPv4 address ("127.1", "2130706433")
    private static final Pattern NUMERIC_LAST_LABEL_PATTERN =
        Pattern.compile("(^|\\.)[0-9]+$");

    /**
     * Canonical form used for storage and comparison: trimmed and
     * lower-cased, since hostnames are case-insensitive.
     */
    public static String normalize(String hostname) {
        return hostname == null
            ? null
            : hostname.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Validates hostname format only (no DNS resolution), so devices
     * that are offline at registration time can still be added.
     */
    public ValidationResult validateFormat(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            return ValidationResult.invalid(
                "Hostname cannot be empty");
        }

        String trimmed = normalize(hostname);

        if (trimmed.length() > 253) {
            return ValidationResult.invalid(
                "Hostname too long (max 253 characters)");
        }

        if (BLOCKED_PATTERN.matcher(trimmed).matches()) {
            log.warn("Blocked hostname attempt: {}", hostname);
            return ValidationResult.invalid(
                "Hostname not allowed: " + hostname);
        }

        boolean isValidIpv4 =
            IPV4_PATTERN.matcher(trimmed).matches();
        boolean isValidHostname =
            HOSTNAME_PATTERN.matcher(trimmed).matches();

        if (!isValidIpv4 && (!isValidHostname
                || NUMERIC_LAST_LABEL_PATTERN.matcher(trimmed).find())) {
            return ValidationResult.invalid(
                "Invalid hostname format: " + hostname);
        }

        return ValidationResult.valid();
    }

    public record ValidationResult(
            boolean isValid, String errorMessage) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(
                String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}
