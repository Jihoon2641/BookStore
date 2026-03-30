package com.bookstore.bookstore_api.admin.application.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringDbStreamResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringJvmStreamResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringMetricResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringOverviewResponse;
import com.bookstore.bookstore_api.admin.application.port.in.MonitoringUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringSseService {

    private static final long SSE_TIMEOUT_MS = 0L;
    private static final long CLIENT_RETRY_MS = 3000L;

    private final MonitoringUseCase monitoringUseCase;

    private final AtomicLong emitterSequence = new AtomicLong(0);
    private final Map<Long, SseEmitter> overviewEmitters = new ConcurrentHashMap<>();
    private final Map<Long, SseEmitter> jvmEmitters = new ConcurrentHashMap<>();
    private final Map<Long, SseEmitter> dbEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe() {
        return subscribeOverview();
    }

    public SseEmitter subscribeOverview() {
        return registerEmitter(overviewEmitters);
    }

    public SseEmitter subscribeJvm() {
        return registerEmitter(jvmEmitters);
    }

    public SseEmitter subscribeDb() {
        return registerEmitter(dbEmitters);
    }

    private SseEmitter registerEmitter(Map<Long, SseEmitter> emitters) {
        long emitterId = emitterSequence.incrementAndGet();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.put(emitterId, emitter);

        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> {
            emitters.remove(emitterId);
            emitter.complete();
        });
        emitter.onError((ex) -> {
            emitters.remove(emitterId);
            emitter.completeWithError(ex);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .reconnectTime(CLIENT_RETRY_MS)
                    .data("connected"));
        } catch (IOException e) {
            emitters.remove(emitterId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @Scheduled(fixedDelayString = "${monitoring.sse.interval-ms:15000}", initialDelayString = "${monitoring.sse.initial-delay-ms:2000}")
    public void broadcastStreams() {
        if (overviewEmitters.isEmpty() && jvmEmitters.isEmpty() && dbEmitters.isEmpty()) {
            return;
        }

        if (!overviewEmitters.isEmpty()) {
            publishOverview();
        }

        if (!jvmEmitters.isEmpty()) {
            publishJvm();
        }

        if (!dbEmitters.isEmpty()) {
            publishDb();
        }
    }

    private void publishOverview() {
        try {
            MonitoringOverviewResponse overview = monitoringUseCase.getOverview();
            broadcast(overviewEmitters, "overview", overview);
        } catch (Exception e) {
            log.warn("Failed to publish monitoring overview SSE event", e);
            broadcast(overviewEmitters, "ping", Instant.now().toString());
        }
    }

    private void publishJvm() {
        try {
            MonitoringJvmStreamResponse response = buildJvmSnapshot();
            broadcast(jvmEmitters, "jvm", response);
        } catch (Exception e) {
            log.warn("Failed to publish JVM SSE event", e);
            broadcast(jvmEmitters, "ping", Instant.now().toString());
        }
    }

    private void publishDb() {
        try {
            MonitoringDbStreamResponse response = buildDbSnapshot();
            broadcast(dbEmitters, "db", response);
        } catch (Exception e) {
            log.warn("Failed to publish DB SSE event", e);
            broadcast(dbEmitters, "ping", Instant.now().toString());
        }
    }

    private MonitoringJvmStreamResponse buildJvmSnapshot() {
        MonitoringMetricResponse heapUsedMetric = safeGetMetric("jvm.memory.used", List.of("area:heap"));
        MonitoringMetricResponse heapMaxMetric = safeGetMetric("jvm.memory.max", List.of("area:heap"));
        MonitoringMetricResponse liveThreadsMetric = safeGetMetric("jvm.threads.live", List.of());
        MonitoringMetricResponse daemonThreadsMetric = safeGetMetric("jvm.threads.daemon", List.of());
        MonitoringMetricResponse loadedClassesMetric = safeGetMetric("jvm.classes.loaded", List.of());
        MonitoringMetricResponse gcPauseMetric = safeGetMetric("jvm.gc.pause", List.of());

        Double heapUsedRaw = pickMeasurementValue(heapUsedMetric, "VALUE");
        Double heapMaxRaw = pickMeasurementValue(heapMaxMetric, "VALUE");
        Double liveThreads = pickMeasurementValue(liveThreadsMetric, "VALUE");
        Double daemonThreads = pickMeasurementValue(daemonThreadsMetric, "VALUE");
        Double loadedClasses = pickMeasurementValue(loadedClassesMetric, "VALUE");
        Double gcPauseRaw = pickMeasurementValue(gcPauseMetric, "MEAN", "MAX", "VALUE");

        return new MonitoringJvmStreamResponse(
                Instant.now(),
                toMegabytes(heapUsedRaw, heapUsedMetric != null ? heapUsedMetric.baseUnit() : null),
                toMegabytes(heapMaxRaw, heapMaxMetric != null ? heapMaxMetric.baseUnit() : null),
                liveThreads,
                daemonThreads,
                loadedClasses,
                toMilliseconds(gcPauseRaw, gcPauseMetric != null ? gcPauseMetric.baseUnit() : null));
    }

    private MonitoringDbStreamResponse buildDbSnapshot() {
        MonitoringMetricResponse activeMetric = safeGetMetric("hikaricp.connections.active", List.of());
        MonitoringMetricResponse idleMetric = safeGetMetric("hikaricp.connections.idle", List.of());
        MonitoringMetricResponse pendingMetric = safeGetMetric("hikaricp.connections.pending", List.of());
        MonitoringMetricResponse maxMetric = safeGetMetric("hikaricp.connections.max", List.of());
        MonitoringMetricResponse timeoutMetric = safeGetMetric("hikaricp.connections.timeout", List.of());
        MonitoringMetricResponse usageMetric = safeGetMetric("hikaricp.connections.usage", List.of());

        Double active = pickMeasurementValue(activeMetric, "VALUE");
        Double idle = pickMeasurementValue(idleMetric, "VALUE");
        Double pending = pickMeasurementValue(pendingMetric, "VALUE");
        Double max = pickMeasurementValue(maxMetric, "VALUE");
        Double timeoutCount = pickMeasurementValue(timeoutMetric, "COUNT", "VALUE");
        Double usageRaw = pickMeasurementValue(usageMetric, "MEAN", "MAX", "TOTAL_TIME", "VALUE");

        return new MonitoringDbStreamResponse(
                Instant.now(),
                active,
                idle,
                pending,
                max,
                timeoutCount,
                toMilliseconds(usageRaw, usageMetric != null ? usageMetric.baseUnit() : null));
    }

    private MonitoringMetricResponse safeGetMetric(String metricName, List<String> tags) {
        try {
            return monitoringUseCase.getMetric(metricName, tags);
        } catch (Exception e) {
            return null;
        }
    }

    private Double pickMeasurementValue(MonitoringMetricResponse metric, String... preferredStatistics) {
        if (metric == null || metric.measurements() == null || metric.measurements().isEmpty()) {
            return null;
        }

        List<String> candidates = Arrays.stream(preferredStatistics).toList();
        for (String candidate : candidates) {
            for (MonitoringMetricResponse.Measurement measurement : metric.measurements()) {
                if (candidate.equals(measurement.statistic()) && measurement.value() != null) {
                    return measurement.value();
                }
            }
        }

        for (MonitoringMetricResponse.Measurement measurement : metric.measurements()) {
            if (measurement.value() != null) {
                return measurement.value();
            }
        }

        return null;
    }

    private Double toMegabytes(Double value, String baseUnit) {
        if (value == null) {
            return null;
        }

        if (baseUnit == null || baseUnit.isBlank()) {
            return value;
        }

        String normalized = baseUnit.toLowerCase();
        if (normalized.contains("byte")) {
            return value / (1024d * 1024d);
        }
        return value;
    }

    private Double toMilliseconds(Double value, String baseUnit) {
        if (value == null) {
            return null;
        }

        if (baseUnit == null || baseUnit.isBlank()) {
            return value;
        }

        String normalized = baseUnit.toLowerCase();
        if (normalized.contains("second")) {
            return value * 1000d;
        }
        return value;
    }

    private void broadcast(Map<Long, SseEmitter> emitters, String eventName, Object payload) {
        List<Long> disconnected = new ArrayList<>();

        for (Map.Entry<Long, SseEmitter> entry : emitters.entrySet()) {
            Long emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload));
            } catch (IOException | IllegalStateException e) {
                disconnected.add(emitterId);
            }
        }

        for (Long emitterId : disconnected) {
            SseEmitter emitter = emitters.remove(emitterId);
            if (emitter != null) {
                emitter.complete();
            }
        }
    }
}
