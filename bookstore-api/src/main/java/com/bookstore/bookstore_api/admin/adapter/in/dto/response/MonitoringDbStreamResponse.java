package com.bookstore.bookstore_api.admin.adapter.in.dto.response;

import java.time.Instant;

public record MonitoringDbStreamResponse(
        Instant generatedAt,
        Double active,
        Double idle,
        Double pending,
        Double max,
        Double timeoutCount,
        Double avgUsageMs) {
}
