package com.bookstore.bookstore_api.admin.adapter.out;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PrometheusQueryAdapter {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public PrometheusQueryAdapter(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${monitoring.prometheus.base-url:http://localhost:9090}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public Optional<Double> queryScalar(String promql) {
        try {
            String responseBody = webClient.get()
                    .uri("/api/v1/query?query={query}", promql)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode result = root.path("data").path("result");
            if (!result.isArray() || result.isEmpty()) {
                return Optional.empty();
            }

            JsonNode valueNode = result.get(0).path("value");
            if (!valueNode.isArray() || valueNode.size() < 2) {
                return Optional.empty();
            }

            String rawValue = valueNode.get(1).asText();
            return Optional.of(Double.parseDouble(rawValue));
        } catch (Exception e) {
            log.warn("Failed to query Prometheus. promql={}", promql, e);
            return Optional.empty();
        }
    }
}
