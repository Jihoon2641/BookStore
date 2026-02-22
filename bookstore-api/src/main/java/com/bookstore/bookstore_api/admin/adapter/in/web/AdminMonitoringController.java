package com.bookstore.bookstore_api.admin.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringHealthResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringMetricNamesResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringMetricResponse;
import com.bookstore.bookstore_api.admin.adapter.in.dto.response.MonitoringOverviewResponse;
import com.bookstore.bookstore_api.admin.application.port.in.MonitoringUseCase;
import com.bookstore.bookstore_api.util.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin Monitoring", description = "관리자 모니터링 API")
@RestController
@RequestMapping("/api/v1/admin/monitoring")
@RequiredArgsConstructor
public class AdminMonitoringController {

    private final MonitoringUseCase monitoringUseCase;

    @Operation(summary = "모니터링 요약", description = "React 대시보드 카드용 요약 데이터(앱 + 호스트 + 디스크/네트워크)")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<MonitoringOverviewResponse>> getOverview() {
        MonitoringOverviewResponse overview = monitoringUseCase.getOverview();
        return ResponseEntity.ok(ApiResponse.success(overview, "모니터링 요약 조회 성공", HttpStatus.OK));
    }

    @Operation(summary = "서비스 상태", description = "서비스 상태, 준비/생존 상태, 업타임 조회")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<MonitoringHealthResponse>> getHealth() {
        MonitoringHealthResponse health = monitoringUseCase.getHealth();
        return ResponseEntity.ok(ApiResponse.success(health, "서비스 상태 조회 성공", HttpStatus.OK));
    }

    @Operation(summary = "메트릭 목록", description = "Actuator에서 노출되는 메트릭 이름 목록")
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<MonitoringMetricNamesResponse>> getMetricNames() {
        MonitoringMetricNamesResponse response = monitoringUseCase.getMetricNames();
        return ResponseEntity.ok(ApiResponse.success(response, "메트릭 목록 조회 성공", HttpStatus.OK));
    }

    @Operation(summary = "메트릭 상세", description = "메트릭 측정값 및 사용 가능한 태그 조회")
    @GetMapping("/metrics/{metricName}")
    public ResponseEntity<ApiResponse<MonitoringMetricResponse>> getMetric(
            @PathVariable String metricName,
            @RequestParam(name = "tag", required = false) List<String> tags) {
        MonitoringMetricResponse response = monitoringUseCase.getMetric(metricName, tags);
        return ResponseEntity.ok(ApiResponse.success(response, "메트릭 상세 조회 성공", HttpStatus.OK));
    }
}
