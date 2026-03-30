package com.bookstore.bookstore_api.admin.adapter.in.dto.response;

import java.time.Instant;

public record MonitoringJvmStreamResponse(
        Instant generatedAt,
        Double heapUsedMb,
        Double heapMaxMb,
        Double liveThreads,
        Double daemonThreads,
        Double loadedClasses,
        Double gcPauseMs) {
}
