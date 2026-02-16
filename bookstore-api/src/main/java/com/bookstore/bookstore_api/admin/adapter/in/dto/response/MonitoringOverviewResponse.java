package com.bookstore.bookstore_api.admin.adapter.in.dto.response;

import java.time.Instant;

public record MonitoringOverviewResponse(
        Instant generatedAt,
        MonitoringHealthResponse status,
        ResourceUsage resources,
        TrafficUsage traffic) {

    public record ResourceUsage(
            Double appCpuPct,
            Double heapUsedPct,
            Double heapUsedMb,
            Double heapMaxMb,
            Double dbPoolActive,
            Double dbPoolMax,
            Double hostCpuPct,
            Double hostMemPct) {
    }

    public record TrafficUsage(
            Double requestsPerSecond,
            Double errorRate5xxPct,
            Double avgLatencyMs) {
    }
}
