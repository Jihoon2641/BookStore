package com.bookstore.bookstore_api.util.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;
    private final UserDetailsService userDetailsService;

    public JwtUtil(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration,
            UserDetailsService userDetailsService) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.userDetailsService = userDetailsService;
    }

    /**
     * JWT 토큰 생성
     * 
     * @param userId 사용자 ID (Subject)
     * @param email  사용자 이메일 (Claim)
     * @param role   사용자 권한 (Claim)
     * @return 생성된 JWT 토큰
     */
    public String createToken(Long userId, String email, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 인증 정보 조회
     * 
     * @param token JWT 토큰
     * @return Authentication 객체
     */
    public Authentication getAuthentication(String token) {
        String email = getEmail(token);
        UserDetails userDetails = userDetailsService
                .loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(userDetails, "",
                userDetails.getAuthorities());
    }

    /**
     * Request의 Header에서 token 값 가져오기
     * 
     * @param request HttpServletRequest
     * @return JWT 토큰 string
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 토큰에서 사용자 ID 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    /**
     * 토큰에서 이메일 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 이메일
     */
    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * 토큰에서 권한 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 권한
     */
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * 토큰 유효성 검증
     * 
     * @param token JWT 토큰
     * @return 유효 여부
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 Claims 추출
     * 
     * @param token JWT 토큰
     * @return Claims 객체
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
