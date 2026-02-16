package com.bookstore.bookstore_api.admin.adapter.in.dto.response;

import java.time.Instant;

public record MonitoringHealthResponse(
        Instant generatedAt,
        String service,
        String readiness,
        String liveness,
        long uptimeSec) {
}
