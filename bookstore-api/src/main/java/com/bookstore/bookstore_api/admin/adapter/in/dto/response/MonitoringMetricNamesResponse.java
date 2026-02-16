package com.bookstore.bookstore_api.admin.adapter.in.dto.response;

import java.time.Instant;
import java.util.List;

public record MonitoringMetricNamesResponse(
        Instant generatedAt,
        List<String> names) {
}
