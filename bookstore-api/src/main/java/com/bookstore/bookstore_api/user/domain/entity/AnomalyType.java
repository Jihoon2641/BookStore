package com.bookstore.bookstore_api.user.domain.entity;

/**
 * 이상 패턴 유형을 정의하는 Enum
 * - Filter/Interceptor: RATE_LIMIT_EXCEEDED, BOT_DETECTED
 * - Security: BRUTE_FORCE_ATTEMPT, UNAUTHORIZED_ACCESS
 * - AOP/Service: UNUSUAL_ACCESS_PATTERN
 */
public enum AnomalyType {
    NONE,                    // 정상
    RATE_LIMIT_EXCEEDED,     // API 호출 속도 제한 초과 (Filter/Interceptor)
    BRUTE_FORCE_ATTEMPT,     // 무차별 대입 공격 시도 (Security - AuthenticationFailureHandler)
    UNAUTHORIZED_ACCESS,     // 무단 접근 시도 (Security - AccessDeniedHandler)
    UNUSUAL_ACCESS_PATTERN,  // 비정상적인 접근 패턴 (AOP/Service에서 분석)
    BOT_DETECTED             // 봇 탐지 (Filter - User-Agent 분석)
}
