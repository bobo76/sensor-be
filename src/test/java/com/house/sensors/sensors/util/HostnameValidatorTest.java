package com.house.sensors.sensors.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HostnameValidatorTest {

    private HostnameValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HostnameValidator();
    }

    @Nested
    class ValidateFormat {

        @Test
        void shouldAcceptValidIPv4() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("192.168.1.100");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        void shouldAcceptValidHostname() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("arduino.local");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        void shouldAcceptHostnameWithHyphens() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("arduino-device-1");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        void shouldRejectNull() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat(null);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).contains("empty");
        }

        @Test
        void shouldRejectEmptyString() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).contains("empty");
        }

        @Test
        void shouldRejectWhitespaceOnly() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("   ");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).contains("empty");
        }

        @Test
        void shouldRejectLocalhost() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("localhost");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("not allowed");
        }

        @Test
        void shouldRejectLocalhostCaseInsensitive() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("LOCALHOST");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("not allowed");
        }

        @Test
        void shouldRejectLoopbackIPv4() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("127.0.0.1");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("not allowed");
        }

        @Test
        void shouldRejectAnyLoopbackAddress() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("127.0.0.5");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("not allowed");
        }

        @Test
        void shouldRejectZeroAddress() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("0.0.0.0");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("not allowed");
        }

        @Test
        void shouldRejectLinkLocalAddresses() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("169.254.1.1");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("not allowed");
        }

        @Test
        void shouldRejectHostnameTooLong() {
            String longHostname = "a".repeat(254);
            HostnameValidator.ValidationResult result =
                validator.validateFormat(longHostname);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("too long");
        }

        @Test
        void shouldRejectInvalidCharacters() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("arduino_device");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("Invalid hostname format");
        }

        @Test
        void shouldRejectHostnameStartingWithHyphen() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("-arduino");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("Invalid hostname format");
        }

        @Test
        void shouldRejectHostnameEndingWithHyphen() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("arduino-");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("Invalid hostname format");
        }

        @Test
        void shouldTrimWhitespace() {
            HostnameValidator.ValidationResult result =
                validator.validateFormat("  192.168.1.100  ");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        void shouldAcceptPrivateIPRanges() {
            assertThat(
                validator.validateFormat("192.168.1.1")
                    .isValid()).isTrue();
            assertThat(
                validator.validateFormat("10.0.0.1")
                    .isValid()).isTrue();
            assertThat(
                validator.validateFormat("172.16.0.1")
                    .isValid()).isTrue();
        }
    }

    @Nested
    class ValidateWithDns {

        @Test
        void shouldAcceptValidPublicIP() {
            HostnameValidator.ValidationResult result =
                validator.validate("8.8.8.8");

            assertThat(result.isValid()).isTrue();
        }

        @Test
        void shouldRejectUnresolvableHostname() {
            HostnameValidator.ValidationResult result =
                validator.validate(
                    "this-hostname-definitely-does-not"
                        + "-exist-12345.com");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("Cannot resolve");
        }

        @Test
        void shouldRejectLocalhost() {
            HostnameValidator.ValidationResult result =
                validator.validate("localhost");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("not allowed");
        }

        @Test
        void shouldDelegateFormatValidation() {
            HostnameValidator.ValidationResult result =
                validator.validate("arduino_invalid");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage())
                .contains("Invalid hostname format");
        }
    }
}
