package com.bookstore.bookstore_api.admin.adapter.in.dto.response;

import java.time.Instant;
import java.util.List;

public record MonitoringMetricResponse(
        Instant generatedAt,
        String name,
        String description,
        String baseUnit,
        List<Measurement> measurements,
        List<AvailableTag> availableTags) {

    public record Measurement(String statistic, Double value) {
    }

    public record AvailableTag(String tag, List<String> values) {
    }
}
