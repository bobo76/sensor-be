package com.house.sensors.sensors.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ClaudeScheduledService {

    private static final int PROCESS_TIMEOUT_MINUTES = 5;
    private static final int OUTPUT_READ_TIMEOUT_SECONDS = 10;
    private static final int MAX_LOG_OUTPUT_LENGTH = 2000;

    private final String prompt;

    public ClaudeScheduledService(
            @Value("${claude.schedule.prompt:hello Claude}")
            String prompt) {
        this.prompt = prompt;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("ClaudeScheduledService is active, "
                + "prompt: \"{}\"", prompt);
    }

    @Scheduled(cron = "${claude.schedule.cron}")
    public void runClaude() {
        log.info("Running Claude CLI with prompt: \"{}\"", prompt);
        try {
            ProcessBuilder pb = createProcessBuilder();
            pb.redirectInput(ProcessBuilder.Redirect.from(
                    new File("/dev/null")));
            pb.redirectErrorStream(true);

            Process process = pb.start();

            CompletableFuture<String> outputFuture =
                    CompletableFuture.supplyAsync(
                            () -> readProcessOutput(process));

            boolean finished = process.waitFor(
                    PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("Claude CLI timed out after {} minutes",
                        PROCESS_TIMEOUT_MINUTES);
                return;
            }

            String output;
            try {
                output = outputFuture.get(
                        OUTPUT_READ_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                output = "[output read timed out]";
            }

            int exitCode = process.exitValue();
            String logOutput = truncateOutput(output);
            if (exitCode == 0) {
                log.info("Claude response:{}", logOutput);
            } else {
                log.error("Claude CLI exited with code {}: {}",
                        exitCode, logOutput);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Claude CLI execution interrupted", e);
        } catch (Exception e) {
            log.error("Failed to run Claude CLI", e);
        }
    }

    ProcessBuilder createProcessBuilder() {
        return new ProcessBuilder("claude", "-p", prompt);
    }

    private String readProcessOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        process.getInputStream(),
                        StandardCharsets.UTF_8))) {
            return reader.lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            return "[output read error: " + e.getMessage() + "]";
        }
    }

    private String truncateOutput(String output) {
        if (output.length() > MAX_LOG_OUTPUT_LENGTH) {
            return output.substring(0, MAX_LOG_OUTPUT_LENGTH)
                    + "... [truncated]";
        }
        return output;
    }
}
