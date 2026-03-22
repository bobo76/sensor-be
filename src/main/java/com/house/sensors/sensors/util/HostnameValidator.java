package com.house.sensors.sensors.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
        "^(localhost|127\\..*|0\\.0\\.0\\.0|::1"
            + "|metadata\\..*|169\\.254\\..*)$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Validates hostname format only (no DNS resolution).
     * Use this for fast validation in hot paths like
     * scheduled polling.
     */
    public ValidationResult validateFormat(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            return ValidationResult.invalid(
                "Hostname cannot be empty");
        }

        String trimmed = hostname.trim().toLowerCase();

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

        if (!isValidIpv4 && !isValidHostname) {
            return ValidationResult.invalid(
                "Invalid hostname format: " + hostname);
        }

        return ValidationResult.valid();
    }

    /**
     * Validates hostname format and resolves via DNS.
     * Use this for user-facing operations like device
     * registration where confirming reachability is valuable.
     */
    public ValidationResult validate(String hostname) {
        ValidationResult formatResult = validateFormat(hostname);
        if (!formatResult.isValid()) {
            return formatResult;
        }

        String trimmed = hostname.trim().toLowerCase();
        try {
            InetAddress address =
                InetAddress.getByName(trimmed);

            if (address.isLoopbackAddress()) {
                log.warn("Blocked loopback address: {}",
                    hostname);
                return ValidationResult.invalid(
                    "Loopback addresses not allowed");
            }

            log.debug("Hostname '{}' resolved to {}",
                hostname, address.getHostAddress());
            return ValidationResult.valid();
        } catch (UnknownHostException e) {
            log.warn("Cannot resolve hostname '{}': {}",
                hostname, e.getMessage());
            return ValidationResult.invalid(
                "Cannot resolve hostname: " + hostname
                    + " - " + e.getMessage());
        }
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
