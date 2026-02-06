package com.bookstore.bookstore_api.common.security;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Bot 탐지 서비스
 * User-Agent 패턴 분석을 통한 봇 탐지
 */
@Service
public class BotDetectionService {

    /**
     * 악의적인 봇으로 의심되는 User-Agent 패턴
     */
    private static final List<Pattern> SUSPICIOUS_BOT_PATTERNS = List.of(
            // 스크래핑 도구
            Pattern.compile("(?i).*scrapy.*"),
            Pattern.compile("(?i).*python-requests.*"),
            Pattern.compile("(?i).*python-urllib.*"),
            Pattern.compile("(?i).*httpclient.*"),
            Pattern.compile("(?i).*java/.*"),
            
            // CLI 도구
            Pattern.compile("(?i).*curl.*"),
            Pattern.compile("(?i).*wget.*"),
            Pattern.compile("(?i).*httpie.*"),
            
            // 자동화 도구
            Pattern.compile("(?i).*selenium.*"),
            Pattern.compile("(?i).*phantomjs.*"),
            Pattern.compile("(?i).*headless.*chrome.*"),
            Pattern.compile("(?i).*puppeteer.*"),
            Pattern.compile("(?i).*playwright.*"),
            
            // 알려진 악성 봇
            Pattern.compile("(?i).*sqlmap.*"),
            Pattern.compile("(?i).*nikto.*"),
            Pattern.compile("(?i).*nmap.*"),
            Pattern.compile("(?i).*masscan.*"),
            Pattern.compile("(?i).*zgrab.*")
    );

    /**
     * 허용되는 봇 (검색엔진 등)
     */
    private static final List<Pattern> ALLOWED_BOT_PATTERNS = List.of(
            Pattern.compile("(?i).*googlebot.*"),
            Pattern.compile("(?i).*bingbot.*"),
            Pattern.compile("(?i).*yandexbot.*"),
            Pattern.compile("(?i).*duckduckbot.*"),
            Pattern.compile("(?i).*slurp.*"),  // Yahoo
            Pattern.compile("(?i).*baiduspider.*")
    );

    /**
     * Bot 여부 탐지
     * 
     * @param userAgent User-Agent 헤더 값
     * @return Bot 탐지 결과
     */
    public BotDetectionResult detect(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new BotDetectionResult(true, "User-Agent가 비어있음");
        }

        // 허용된 봇인지 확인
        for (Pattern pattern : ALLOWED_BOT_PATTERNS) {
            if (pattern.matcher(userAgent).matches()) {
                return new BotDetectionResult(false, null);
            }
        }

        // 의심스러운 봇 패턴 확인
        for (Pattern pattern : SUSPICIOUS_BOT_PATTERNS) {
            if (pattern.matcher(userAgent).matches()) {
                return new BotDetectionResult(true, "의심스러운 User-Agent 패턴: " + pattern.pattern());
            }
        }

        return new BotDetectionResult(false, null);
    }

    /**
     * Bot 탐지 결과
     */
    public record BotDetectionResult(
            boolean isBot,
            String reason
    ) {
    }

}
