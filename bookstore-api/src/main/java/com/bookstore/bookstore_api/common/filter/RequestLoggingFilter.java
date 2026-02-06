package com.bookstore.bookstore_api.common.filter;

import com.bookstore.bookstore_api.common.ratelimit.RateLimiterService;
import com.bookstore.bookstore_api.common.security.BotDetectionService;
import com.bookstore.bookstore_api.common.security.BotDetectionService.BotDetectionResult;
import com.bookstore.bookstore_api.user.application.port.out.UserLogRepository;
import com.bookstore.bookstore_api.user.domain.entity.AnomalyType;
import com.bookstore.bookstore_api.user.domain.entity.LogLevel;
import com.bookstore.bookstore_api.user.domain.entity.UserLogEntity;

import io.github.bucket4j.ConsumptionProbe;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청 로깅 및 이상 패턴 감지 필터
 * - Bot Detection
 * - Rate Limiting
 * - 요청 로깅
 */
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final BotDetectionService botDetectionService;
    private final UserLogRepository userLogRepository;

    public RequestLoggingFilter(
            RateLimiterService rateLimiterService,
            BotDetectionService botDetectionService,
            UserLogRepository userLogRepository) {
        this.rateLimiterService = rateLimiterService;
        this.botDetectionService = botDetectionService;
        this.userLogRepository = userLogRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUrl = request.getRequestURI();
        String requestMethod = request.getMethod();
        String queryParams = request.getQueryString();

        // Bot Detection
        BotDetectionResult botResult = botDetectionService.detect(userAgent);
        if (botResult.isBot()) {
            handleBotDetected(request, response, clientIp, userAgent, requestUrl, requestMethod, botResult.reason());
            return;
        }

        // Rate Limit 체크
        ConsumptionProbe probe = rateLimiterService.tryConsumeAndReturnRemaining(clientIp);
        if (!probe.isConsumed()) {
            handleRateLimitExceeded(request, response, clientIp, userAgent, requestUrl, requestMethod, probe);
            return;
        }

        // Rate Limit 헤더 추가
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

        // 요청 처리
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 4. 요청 로깅 (정상 요청)
            long responseTime = System.currentTimeMillis() - startTime;
            logRequest(clientIp, userAgent, requestUrl, requestMethod, queryParams,
                    response.getStatus(), responseTime, false, AnomalyType.NONE, null);
        }
    }

    /**
     * Bot 탐지 시 처리
     */
    private void handleBotDetected(
            HttpServletRequest request,
            HttpServletResponse response,
            String clientIp,
            String userAgent,
            String requestUrl,
            String requestMethod,
            String reason) throws IOException {

        log.warn("Bot detected - IP: {}, User-Agent: {}, Reason: {}", clientIp, userAgent, reason);

        // 로그 저장
        logRequest(clientIp, userAgent, requestUrl, requestMethod, request.getQueryString(),
                HttpServletResponse.SC_FORBIDDEN, 0L, true, AnomalyType.BOT_DETECTED, reason);

        // 403 Forbidden 응답
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Access denied\", \"message\": \"Bot detected\"}");
    }

    /**
     * Rate Limit 초과 시 처리
     */
    private void handleRateLimitExceeded(
            HttpServletRequest request,
            HttpServletResponse response,
            String clientIp,
            String userAgent,
            String requestUrl,
            String requestMethod,
            ConsumptionProbe probe) throws IOException {

        long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        log.warn("Rate limit exceeded - IP: {}, Wait: {}s", clientIp, waitSeconds);

        // 로그 저장
        String reason = String.format("Rate limit exceeded. Wait %d seconds.", waitSeconds);
        logRequest(clientIp, userAgent, requestUrl, requestMethod, request.getQueryString(),
                429, 0L, true, AnomalyType.RATE_LIMIT_EXCEEDED, reason);

        // 429 Too Many Requests 응답
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.getWriter().write(
                "{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded\", \"retryAfter\": " + waitSeconds + "}");
    }

    /**
     * 요청 로깅
     */
    private void logRequest(
            String clientIp,
            String userAgent,
            String requestUrl,
            String requestMethod,
            String queryParams,
            int responseStatus,
            long responseTimeMs,
            boolean isAnomalous,
            AnomalyType anomalyType,
            String anomalyDescription) {

        try {
            UserLogEntity log = UserLogEntity.builder()
                    .logLevel(isAnomalous ? LogLevel.WARN : LogLevel.INFO)
                    .message(buildLogMessage(requestMethod, requestUrl, responseStatus, isAnomalous))
                    .requestUrl(requestUrl)
                    .requestMethod(requestMethod)
                    .queryParams(queryParams)
                    .responseStatus(responseStatus)
                    .responseTimeMs(responseTimeMs)
                    .ipAddress(clientIp)
                    .userAgent(truncate(userAgent, 500))
                    .isAnomalous(isAnomalous)
                    .anomalyType(anomalyType)
                    .anomalyDescription(anomalyDescription)
                    .build();

            userLogRepository.save(log);
        } catch (Exception e) {
            // 로깅 실패 시 요청 처리에 영향을 주지 않음
            RequestLoggingFilter.log.error("Failed to save request log", e);
        }
    }

    /**
     * 로그 메시지 생성
     */
    private String buildLogMessage(String method, String url, int status, boolean isAnomalous) {
        if (isAnomalous) {
            return String.format("[ANOMALY] %s %s - %d", method, url, status);
        }
        return String.format("%s %s - %d", method, url, status);
    }

    /**
     * 클라이언트 IP 추출 (프록시 환경 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For는 여러 IP가 있을 수 있음 (첫 번째가 실제 클라이언트)
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 문자열 길이 제한
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    /**
     * 특정 URL 패턴 필터 제외 (Swagger, 정적 리소스 등)
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/favicon.ico") ||
                path.startsWith("/actuator");
    }

}
