package com.house.sensors.sensors.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaudeScheduledServiceTest {

    private ClaudeScheduledService service;

    @Mock
    private ProcessBuilder processBuilder;

    @Mock
    private Process process;

    @BeforeEach
    void setUp() throws Exception {
        service = spy(new ClaudeScheduledService("test prompt"));
        doReturn(processBuilder).when(service)
                .createProcessBuilder();
        when(processBuilder.redirectInput(
                any(ProcessBuilder.Redirect.class)))
                .thenReturn(processBuilder);
        when(processBuilder.redirectErrorStream(anyBoolean()))
                .thenReturn(processBuilder);
        when(processBuilder.start()).thenReturn(process);
    }

    private void setProcessOutput(String output) {
        InputStream inputStream = new ByteArrayInputStream(
                output.getBytes(StandardCharsets.UTF_8));
        when(process.getInputStream()).thenReturn(inputStream);
    }

    @Test
    @DisplayName("Should log response on successful execution")
    void shouldLogResponseOnSuccessfulExecution()
            throws Exception {
        setProcessOutput("Hello from Claude");
        when(process.waitFor(anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(process.exitValue()).thenReturn(0);

        service.runClaude();

        verify(process).exitValue();
        verify(process, never()).destroyForcibly();
    }

    @Test
    @DisplayName("Should log error on non-zero exit code")
    void shouldLogErrorOnNonZeroExitCode() throws Exception {
        setProcessOutput("Error output");
        when(process.waitFor(anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(process.exitValue()).thenReturn(1);

        service.runClaude();

        verify(process).exitValue();
        verify(process, never()).destroyForcibly();
    }

    @Test
    @DisplayName("Should destroy process on timeout")
    void shouldDestroyProcessOnTimeout() throws Exception {
        setProcessOutput("");
        when(process.waitFor(anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        service.runClaude();

        verify(process).destroyForcibly();
        verify(process, never()).exitValue();
    }

    @Test
    @DisplayName("Should handle process start failure")
    void shouldHandleProcessStartFailure() throws Exception {
        when(processBuilder.start())
                .thenThrow(new IOException("Command not found"));

        service.runClaude();

        verify(process, never()).waitFor(
                anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Should restore interrupt flag on interruption")
    void shouldRestoreInterruptFlagOnInterruption()
            throws Exception {
        setProcessOutput("");
        when(process.waitFor(anyLong(), any(TimeUnit.class)))
                .thenThrow(new InterruptedException("interrupted"));

        service.runClaude();

        assertTrue(Thread.currentThread().isInterrupted());
    }
}
