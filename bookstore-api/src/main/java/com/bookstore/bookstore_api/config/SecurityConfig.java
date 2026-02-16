package com.bookstore.bookstore_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.bookstore.bookstore_api.common.filter.RequestLoggingFilter;
import com.bookstore.bookstore_api.common.security.JwtAuthenticationFilter;
import com.bookstore.bookstore_api.common.ratelimit.RateLimiterService;
import com.bookstore.bookstore_api.common.security.BotDetectionService;
import com.bookstore.bookstore_api.user.application.port.out.UserLogRepository;
import com.bookstore.bookstore_api.util.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final RateLimiterService rateLimiterService;
    private final BotDetectionService botDetectionService;
    private final UserLogRepository userLogRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil);
        RequestLoggingFilter requestLoggingFilter = new RequestLoggingFilter(rateLimiterService, botDetectionService,
                userLogRepository);
        AuthorizationManager<RequestAuthorizationContext> monitoringAccessManager = monitoringAccessManager();

        http
                .httpBasic(httpBasic -> httpBasic.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus",
                                "/actuator/metrics/**")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/monitoring/**").access(monitoringAccessManager)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/user/login", "/api/v1/user/signup", "/api/v1/orders").permitAll()
                        .requestMatchers("/api/v1/books/**").permitAll()
                        .anyRequest().authenticated())
                // RequestLoggingFilter -> JwtAuthenticationFilter ->
                // UsernamePasswordAuthenticationFilter 순서
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestLoggingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_SUPER_ADMIN > ROLE_LEVEL_2
                ROLE_LEVEL_2 > ROLE_LEVEL_1
                ROLE_LEVEL_1 > ROLE_USER
                """);
    }

    private AuthorizationManager<RequestAuthorizationContext> monitoringAccessManager() {
        AuthorityAuthorizationManager<RequestAuthorizationContext> accessManager = AuthorityAuthorizationManager
                .hasRole("LEVEL_1");
        accessManager.setRoleHierarchy(roleHierarchy());
        return accessManager;
    }
}
