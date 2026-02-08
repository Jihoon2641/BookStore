package com.bookstore.bookstore_api.user.domain.entity;

import com.bookstore.bookstore_api.util.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_logs", indexes = {
    @Index(name = "idx_user_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_user_logs_ip_address", columnList = "ip_address"),
    @Index(name = "idx_user_logs_created_at", columnList = "created_at"),
    @Index(name = "idx_user_logs_is_anomalous", columnList = "is_anomalous"),
    @Index(name = "idx_user_logs_jwt_id", columnList = "jwt_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class UserLogEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== 기본 로그 정보 ====================

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 10)
    private LogLevel logLevel;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    // ==================== 요청 정보 ====================

    @Column(name = "request_url", length = 500)
    private String requestUrl;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "query_params", length = 1000)
    private String queryParams;

    // ==================== 응답 정보 ====================

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    // ==================== 사용자 정보 ====================

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "jwt_id", length = 100)
    private String jwtId;  // JWT의 jti claim (토큰 식별용)

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    // ==================== 이상 패턴 감지 ====================

    @Builder.Default
    @Column(name = "is_anomalous", nullable = false)
    private Boolean isAnomalous = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", length = 30)
    private AnomalyType anomalyType = AnomalyType.NONE;

    @Column(name = "anomaly_description", length = 500)
    private String anomalyDescription;

    @Builder.Default
    @Column(name = "threat_score")
    private Integer threatScore = 0;  // 0-100 범위의 위협 점수

    @Builder.Default
    @Column(name = "request_count_in_window")
    private Integer requestCountInWindow = 1;  // 시간 윈도우 내 요청 횟수

    @Builder.Default
    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;  // 연속 실패 시도 횟수

    @Builder.Default
    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;  // 차단 여부

    @Column(name = "blocked_reason", length = 255)
    private String blockedReason;

    // ==================== 디버깅 정보 ====================

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "class_name", length = 255)
    private String className;

    @Column(name = "method_name", length = 100)
    private String methodName;

}
