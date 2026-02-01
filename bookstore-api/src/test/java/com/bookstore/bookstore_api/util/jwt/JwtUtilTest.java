package com.bookstore.bookstore_api.util.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UserDetailsService;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret = "VlwEyVlPKZ6yV9yVlwEyVlPKZ6yV9yVlwEyVlPKZ6yV9yVlwEyVlPKZ6yV9y";
    private long expiration = 3600000;

    @Mock
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = org.mockito.Mockito
                .mock(UserDetailsService.class);
        jwtUtil = new JwtUtil(secret, expiration, userDetailsService);
    }

    @Test
    @DisplayName("토큰 생성 및 정보 추출 테스트")
    void createTokenAndGetClaims() {
        // given
        Long userId = 1L;
        String email = "test@example.com";
        String role = "USER";

        // when
        String token = jwtUtil.createToken(userId, email, role);

        // then
        assertThat(token).isNotNull();
        assertThat(jwtUtil.getUserId(token)).isEqualTo(userId);
        assertThat(jwtUtil.getEmail(token)).isEqualTo(email);
    }

    @Test
    @DisplayName("유효한 토큰 검증 테스트")
    void validateValidToken() {
        // given
        String token = jwtUtil.createToken(1L, "test@example.com", "USER");

        // when
        boolean isValid = jwtUtil.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰 검증 테스트")
    void validateExpiredToken() throws InterruptedException {
        // given
        long shortExpiration = 1;
        JwtUtil shortLiveJwtUtil = new JwtUtil(secret, shortExpiration, userDetailsService);
        String token = shortLiveJwtUtil.createToken(1L, "test@example.com", "USER");

        Thread.sleep(10);

        // when
        boolean isValid = shortLiveJwtUtil.validateToken(token);

        // then
        assertThat(isValid).isFalse();
    }
}
