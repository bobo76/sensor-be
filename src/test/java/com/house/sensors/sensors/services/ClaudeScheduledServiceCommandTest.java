package com.house.sensors.sensors.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Kept separate from ClaudeScheduledServiceTest: that class spies
// createProcessBuilder() and stubs it strictly in @BeforeEach.
class ClaudeScheduledServiceCommandTest {

    @Test
    @DisplayName("Should pass configured model and prompt to Claude CLI")
    void shouldPassConfiguredModelAndPrompt() {
        ClaudeScheduledService service =
                new ClaudeScheduledService("test prompt", "haiku");

        assertThat(service.createProcessBuilder().command())
                .containsExactly(
                        "claude", "--model", "haiku", "-p", "test prompt");
    }
}
