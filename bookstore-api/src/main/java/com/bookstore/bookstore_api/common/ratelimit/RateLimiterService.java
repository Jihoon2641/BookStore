package com.bookstore.bookstore_api.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 기반 Rate Limiting 서비스
 * Bucket4j Token Bucket 알고리즘 사용
 */
@Service
public class RateLimiterService {

    /**
     * IP별 Bucket 저장소
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Rate Limit 설정: 분당 100건
     */
    private static final int REQUESTS_PER_MINUTE = 100;

    /**
     * 요청 허용 여부 확인
     * 
     * @param ip 클라이언트 IP 주소
     * @return 요청 허용 여부
     */
    public boolean tryConsume(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, this::createBucket);
        return bucket.tryConsume(1);
    }

    /**
     * 요청 허용 여부 확인 및 상세 정보 반환
     * 
     * @param ip 클라이언트 IP 주소
     * @return ConsumptionProbe (남은 토큰, 리필까지 남은 시간 등)
     */
    public ConsumptionProbe tryConsumeAndReturnRemaining(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, this::createBucket);
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    /**
     * 특정 IP의 남은 토큰 수 조회
     * 
     * @param ip 클라이언트 IP 주소
     * @return 남은 토큰 수
     */
    public long getAvailableTokens(String ip) {
        Bucket bucket = buckets.get(ip);
        return bucket != null ? bucket.getAvailableTokens() : REQUESTS_PER_MINUTE;
    }

    /**
     * 특정 IP의 Bucket 초기화 (차단 해제 시 사용)
     * 
     * @param ip 클라이언트 IP 주소
     */
    public void resetBucket(String ip) {
        buckets.remove(ip);
    }

    /**
     * 모든 Bucket 초기화
     */
    public void resetAllBuckets() {
        buckets.clear();
    }

    /**
     * 새로운 Bucket 생성
     * Token Bucket: 분당 100개 토큰, 초기 100개
     */
    private Bucket createBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(REQUESTS_PER_MINUTE)
                .refillGreedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * 현재 관리 중인 IP 수 조회 (모니터링용)
     */
    public int getTrackedIpCount() {
        return buckets.size();
    }

}
