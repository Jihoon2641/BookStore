package com.bookstore.bookstore_api.admin.application.service;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.MetricDescriptor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringHealthResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringMetricNamesResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringMetricResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringOverviewResponse;
import com.bookstore.bookstore_api.admin.adapter.out.PrometheusQueryAdapter;
import com.bookstore.bookstore_api.admin.application.port.in.MonitoringUseCase;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MonitoringService implements MonitoringUseCase {

    private static final double BYTES_TO_MB = 1024d * 1024d;

    private final MetricsEndpoint metricsEndpoint;
    private final MeterRegistry meterRegistry;
    private final HealthEndpoint healthEndpoint;
    private final PrometheusQueryAdapter prometheusQueryAdapter;

    @Override
    public MonitoringOverviewResponse getOverview() {
        MonitoringHealthResponse health = getHealth();

        Double appCpuPct = multiplyBy100(getGaugeValue("process.cpu.usage"));
        Double heapUsedBytes = getGaugeValue("jvm.memory.used", "area", "heap");
        Double heapMaxBytes = getGaugeValue("jvm.memory.max", "area", "heap");
        Double heapUsedPct = divideAndMultiply(heapUsedBytes, heapMaxBytes, 100d);
        Double dbPoolActive = getGaugeValue("hikaricp.connections.active");
        Double dbPoolMax = getGaugeValue("hikaricp.connections.max");
        Double hostCpuPct = getHostCpuPct();
        Double hostMemPct = getHostMemPct();

        TrafficMetrics trafficMetrics = calculateTrafficMetrics(health.uptimeSec());

        MonitoringOverviewResponse.ResourceUsage resources = new MonitoringOverviewResponse.ResourceUsage(
                round(appCpuPct),
                round(heapUsedPct),
                round(divide(heapUsedBytes, BYTES_TO_MB)),
                round(divide(heapMaxBytes, BYTES_TO_MB)),
                round(dbPoolActive),
                round(dbPoolMax),
                round(hostCpuPct),
                round(hostMemPct));

        MonitoringOverviewResponse.TrafficUsage traffic = new MonitoringOverviewResponse.TrafficUsage(
                round(trafficMetrics.requestsPerSecond()),
                round(trafficMetrics.errorRate5xxPct()),
                round(trafficMetrics.avgLatencyMs()));

        return new MonitoringOverviewResponse(Instant.now(), health, resources, traffic);
    }

    @Override
    public MonitoringHealthResponse getHealth() {
        HealthComponent healthComponent = healthEndpoint.health();
        String service = resolveServiceStatus(healthComponent);
        long uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return new MonitoringHealthResponse(Instant.now(), service, uptimeSec);
    }

    @Override
    public MonitoringMetricNamesResponse getMetricNames() {
        List<String> names = new ArrayList<>(metricsEndpoint.listNames().getNames());
        names.sort(Comparator.naturalOrder());
        return new MonitoringMetricNamesResponse(Instant.now(), names);
    }

    @Override
    public MonitoringMetricResponse getMetric(String metricName, List<String> tags) {
        List<String> normalizedTags = normalizeTags(tags);
        MetricDescriptor metric = metricsEndpoint.metric(metricName, normalizedTags);

        if (metric == null) {
            throw new ResponseStatusException(NOT_FOUND, "Metric not found: " + metricName);
        }

        List<MonitoringMetricResponse.Measurement> measurements = metric.getMeasurements().stream()
                .map(measurement -> new MonitoringMetricResponse.Measurement(
                        measurement.getStatistic().name(),
                        round(measurement.getValue())))
                .toList();

        List<MonitoringMetricResponse.AvailableTag> availableTags = metric.getAvailableTags().stream()
                .map(tag -> new MonitoringMetricResponse.AvailableTag(
                        tag.getTag(),
                        tag.getValues().stream().sorted().toList()))
                .toList();

        return new MonitoringMetricResponse(
                Instant.now(),
                metric.getName(),
                metric.getDescription(),
                metric.getBaseUnit(),
                measurements,
                availableTags);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        List<String> normalized = new ArrayList<>();
        for (String rawTag : tags) {
            if (!StringUtils.hasText(rawTag)) {
                continue;
            }

            String[] parts = rawTag.split(":", 2);
            if (parts.length != 2 || !StringUtils.hasText(parts[0]) || !StringUtils.hasText(parts[1])) {
                throw new ResponseStatusException(BAD_REQUEST, "Invalid tag format. Use key:value");
            }

            normalized.add(parts[0].trim() + ":" + parts[1].trim());
        }
        return normalized;
    }

    private TrafficMetrics calculateTrafficMetrics(long uptimeSec) {
        var timers = meterRegistry.find("http.server.requests").timers();
        if (timers.isEmpty() || uptimeSec <= 0) {
            return new TrafficMetrics(null, null, null);
        }

        double totalCount = timers.stream().mapToDouble(Timer::count).sum();
        double error5xxCount = timers.stream()
                .filter(timer -> is5xx(timer.getId().getTag("status")))
                .mapToDouble(Timer::count)
                .sum();
        double totalTimeSec = timers.stream()
                .mapToDouble(timer -> timer.totalTime(java.util.concurrent.TimeUnit.SECONDS))
                .sum();

        Double rps = totalCount / uptimeSec;
        Double errorRate = totalCount > 0 ? (error5xxCount / totalCount) * 100d : null;
        Double avgLatencyMs = totalCount > 0 ? (totalTimeSec / totalCount) * 1000d : null;
        return new TrafficMetrics(rps, errorRate, avgLatencyMs);
    }

    private boolean is5xx(String status) {
        return status != null && status.startsWith("5");
    }

    private Double getHostCpuPct() {
        String query = "100 - (avg by(instance)(rate(node_cpu_seconds_total{mode=\"idle\"}[1m])) * 100)";
        return prometheusQueryAdapter.queryScalar(query).orElse(null);
    }

    private Double getHostMemPct() {
        String query = "(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100";
        return prometheusQueryAdapter.queryScalar(query).orElse(null);
    }

    private String resolveServiceStatus(HealthComponent healthComponent) {
        if (healthComponent == null) {
            return "UNKNOWN";
        }

        try {
            Object status = healthComponent.getClass().getMethod("getStatus").invoke(healthComponent);
            if (status != null) {
                String code = Objects.toString(status, "UNKNOWN");
                return code.toUpperCase(Locale.ROOT);
            }
        } catch (ReflectiveOperationException ignored) {

        }

        return "UNKNOWN";
    }

    private Double getGaugeValue(String meterName, String... tagKeyValuePairs) {
        if (tagKeyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Tag key/value pairs must be even");
        }

        var search = meterRegistry.find(meterName);
        for (int i = 0; i < tagKeyValuePairs.length; i += 2) {
            search = search.tag(tagKeyValuePairs[i], tagKeyValuePairs[i + 1]);
        }

        var gauge = search.gauge();
        return gauge == null ? null : gauge.value();
    }

    private Double divideAndMultiply(Double numerator, Double denominator, Double factor) {
        if (numerator == null || denominator == null || denominator == 0) {
            return null;
        }
        return (numerator / denominator) * factor;
    }

    private Double divide(Double value, double denominator) {
        if (value == null) {
            return null;
        }
        return value / denominator;
    }

    private Double multiplyBy100(Double value) {
        if (value == null) {
            return null;
        }
        return value * 100d;
    }

    private Double round(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0d) / 100.0d;
    }

    private record TrafficMetrics(Double requestsPerSecond, Double errorRate5xxPct, Double avgLatencyMs) {
    }
}
