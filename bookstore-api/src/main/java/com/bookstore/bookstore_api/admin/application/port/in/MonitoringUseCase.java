package com.bookstore.bookstore_api.admin.application.port.in;

import java.util.List;

import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringHealthResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringMetricNamesResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringMetricResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringOverviewResponse;

public interface MonitoringUseCase {

    MonitoringOverviewResponse getOverview();

    MonitoringHealthResponse getHealth();

    MonitoringMetricNamesResponse getMetricNames();

    MonitoringMetricResponse getMetric(String metricName, List<String> tags);
}
