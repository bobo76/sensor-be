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
            "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)*[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$"
    );

    // IPv4 pattern
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // Blocked patterns for security (localhost, private IPs that aren't Arduino devices)
    private static final Pattern BLOCKED_PATTERN = Pattern.compile(
            "^(localhost|127\\..*|0\\.0\\.0\\.0|::1|metadata\\..*|169\\.254\\..*)$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Validates a hostname for Arduino device connections.
     * Accepts valid DNS names and IP addresses, blocks localhost and metadata services.
     *
     * @param hostname The hostname to validate
     * @return ValidationResult with success status and error message if invalid
     */
    public ValidationResult validate(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            return ValidationResult.invalid("Hostname cannot be empty");
        }

        String trimmed = hostname.trim().toLowerCase();

        // Check length
        if (trimmed.length() > 253) {
            return ValidationResult.invalid("Hostname too long (max 253 characters)");
        }

        // Check for blocked patterns (localhost, metadata services)
        if (BLOCKED_PATTERN.matcher(trimmed).matches()) {
            log.warn("Blocked hostname attempt: {}", hostname);
            return ValidationResult.invalid(
                "Hostname not allowed: " + hostname);
        }

        // Validate format (IPv4 or DNS name)
        boolean isValidIpv4 = IPV4_PATTERN.matcher(trimmed).matches();
        boolean isValidHostname = HOSTNAME_PATTERN.matcher(trimmed).matches();

        if (!isValidIpv4 && !isValidHostname) {
            return ValidationResult.invalid("Invalid hostname format: " + hostname);
        }

        // Additional check: try to resolve the hostname
        try {
            InetAddress address = InetAddress.getByName(trimmed);

            // Block loopback addresses
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

    public record ValidationResult(boolean isValid, String errorMessage) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}
